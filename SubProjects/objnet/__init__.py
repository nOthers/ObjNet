#!/usr/local/bin/python3
import json
import numpy
import socket
import itertools
import threading
numpy.seterr(all='ignore')
LOCK = threading.RLock()
SESSIONS = {}


def hashCode(string):
    h = numpy.int32(0)
    for char in string:
        h = numpy.int32(31) * h + numpy.uint16(ord(char))
    return int(h)


# 应用名映射端口号
def get_port_by_name(name):
    return 2**15 + (hashCode(name) % 2**15)


# 访问远程设备反射服务
def request(host, name_or_port, object, method, *params):
    port = get_port_by_name(name_or_port) if isinstance(name_or_port, str) else name_or_port
    addr = f'{host}:{port}'
    with LOCK:
        if addr not in SESSIONS:
            SESSIONS[addr] = Session(host, port)
        session = SESSIONS[addr]
    return session.request(object, method, *params)


class RemoteException(BaseException):
    pass


class Session:
    def __init__(self, host, port):
        try:
            self.socket = socket.create_connection((host, port,))
        except BaseException:
            raise RemoteException('remote deny connection')
        self.closed = False
        self.buffer = b''
        self.unique = itertools.count(1)
        self.events = {}
        threading.Thread(target=self.run, daemon=True).start()

    # 请求反射服务
    def request(self, object, method, *params):
        xid = next(self.unique) % 2**16
        event = threading.Event()
        self.events[xid] = event
        self.socket.send(json.dumps({
            'xid': xid,
            'object': object,
            'method': method,
            'params': params,
        }).encode())
        self.socket.send(b'\x00')
        event.wait()
        if event.e is not None:
            raise RemoteException(event.e)
        return event.r

    # 回调反射服务
    def response(self, data):
        pack = json.loads(data.decode())
        event = self.events.pop(pack['xid'], None)
        if event is not None:
            event.r = pack['r']
            event.e = pack['e']
            event.set()

    # 线程轮询消息
    def run(self):
        while True:
            try:
                b = self.socket.recv(1024)
            except socket.timeout:
                continue
            except ConnectionError:
                b = b''
            if not b:
                break
            self.buffer += b
            split = self.buffer.split(b'\x00')
            self.buffer = split.pop()
            for s in split:
                self.response(s)
        self.closed = True

    # 关闭连接
    def close(self):
        self.socket.close()
        while not self.closed:
            pass
