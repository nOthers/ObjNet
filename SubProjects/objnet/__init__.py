#!/usr/local/bin/python3
import re
import os
import json
import numpy
import socket
import itertools
import threading
numpy.seterr(all='ignore')
UNIQUE = itertools.count(1)
EVENTS = {}


class RemoteException(BaseException):
    pass


class INetwork:
    # 访问对象网络服务
    def request(self, pkgname, object, method, *params, timeout=None):
        xid = next(UNIQUE) % 2**16
        event = threading.Event()
        EVENTS[xid] = event
        self.send(pkgname, {
            'xid': xid,
            'object': object,
            'method': method,
            'params': params,
        })
        if not event.wait(timeout=timeout):
            raise TimeoutError('remote response timeout')
        if event.e is not None:
            raise RemoteException(event.e)
        return event.r

    # 发送数据
    def send(self, pkgname, data):
        raise NotImplementedError

    # 数据回调
    def recv(self, data):
        event = EVENTS.pop(data['xid'], None)
        if event is not None:
            event.r = data['r']
            event.e = data['e']
            event.set()


# 获取默认设备号
def get_default_serial():
    serial = None
    text = os.popen('adb devices').read()
    for line in text.split('\n'):
        if line.endswith('device'):
            serial = line.split()[0]
            break
    return serial


# 包名映射广播号
def get_action_by_pkgname(pkgname):
    return pkgname + '/objnet'


class BroadcastNetwork(INetwork):
    def __init__(self, serial=None):
        if serial is None:
            serial = get_default_serial()
            assert serial is not None, 'default device not found'
        self.serial = serial

    def send(self, pkgname, data):
        action = get_action_by_pkgname(pkgname)
        data = json.dumps(data).encode().hex()
        text = os.popen(f'adb -s {self.serial} shell am broadcast -a {action} -e data {data}').read()
        m = re.search(r', data="([0-9a-fA-F]*)"', text)
        if not m:
            raise EOFError('remote response stopped')
        data = json.loads(bytes.fromhex(m.group(1)).decode())
        self.recv(data)


# 哈希算法
def hashCode(string):
    h = numpy.int32(0)
    for char in string:
        h = numpy.int32(31) * h + numpy.uint16(ord(char))
    return int(h)


# 包名映射端口号
def get_port_by_pkgname(pkgname):
    return 2**15 + (hashCode(pkgname) % 2**15)


class SocketNetwork(INetwork):
    def __init__(self, host):
        self.host = host

    def send(self, pkgname, data):
        port = get_port_by_pkgname(pkgname)
        sock = socket.create_connection((self.host, port,))
        try:
            sock.sendall(json.dumps(data).encode())
            sock.sendall(b'\x00')
            buffer = b''
            while True:
                try:
                    b = sock.recv(1024)
                except socket.timeout:
                    continue
                except ConnectionError:
                    b = b''
                if not b:
                    raise EOFError('remote response stopped')
                buffer += b
                if b'\x00' in buffer:
                    buffer = buffer.split(b'\x00')[0]
                    self.recv(json.loads(buffer.decode()))
                    break
        finally:
            sock.close()
