package utopia.android.test.objnet;

import android.app.Application;

import utopia.android.util.objnet.ObjectNetwork;
import utopia.android.util.objnet.impl.ActualObject;
import utopia.android.util.objnet.impl.BroadcastNetwork;
import utopia.android.util.objnet.impl.SocketNetwork;

public class MainApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        ObjectNetwork.startup(this, new BroadcastNetwork());
        ObjectNetwork.startup(this, new SocketNetwork());
        ObjectNetwork.register("application", new ActualObject(this));
    }

    public String ping() {
        return "pong";
    }
}
