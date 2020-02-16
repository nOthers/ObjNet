package utopia.android.util.objnet.impl;

import android.content.Context;

import utopia.android.util.objnet.INetwork;
import utopia.android.util.objnet.ObjectNetwork;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SocketNetwork implements INetwork, Runnable {
    private ServerSocket mServerSocket;
    private ExecutorService mExecutorService;

    @Override
    public void onStartUp(Context context) {
        try {
            int port = getPortByPackageName(context.getPackageName());
            mServerSocket = new ServerSocket(port, 1);
            mExecutorService = Executors.newFixedThreadPool(1);
            new Thread(this).start();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        do {
            try {
                new Session(mServerSocket.accept(), mExecutorService);
            } catch (IOException e) {
                break;
            }
        } while (true);
    }

    @Override
    public void onShutDown() {
        try {
            mServerSocket.close();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class Session implements Runnable {
        private Socket mSocket;
        private ExecutorService mExecutor;

        public Session(Socket socket, ExecutorService executor) {
            mSocket = socket;
            mExecutor = executor;
            new Thread(this).start();
        }

        @Override
        public void run() {
            try {
                InputStream input = mSocket.getInputStream();
                final OutputStream output = mSocket.getOutputStream();
                List<Byte> buffer = new LinkedList<>();
                int b;
                while ((b = input.read()) != -1) {
                    if (b != 0) {
                        buffer.add((byte) b);
                        continue;
                    }
                    final byte[] bytes = new byte[buffer.size()];
                    for (int i = 0; i < bytes.length; i++) {
                        bytes[i] = buffer.remove(0);
                    }
                    mExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            String requestJson = new String(bytes);
                            String responseJson = ObjectNetwork.doResponse(requestJson);
                            try {
                                output.write(responseJson.getBytes());
                                output.write(0);
                            } catch (IOException ignore) {
                            }
                        }
                    });
                }
            } catch (IOException e) {
                try {
                    mSocket.close();
                } catch (IOException ignore) {
                }
            }
        }
    }

    public static int getPortByPackageName(String packageName) {
        return 32768 + (packageName.hashCode() % 32768);
    }
}
