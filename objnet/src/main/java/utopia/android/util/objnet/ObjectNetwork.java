package utopia.android.util.objnet;

import android.content.Context;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ObjectNetwork {
    private static final List<INetwork> networkList = new LinkedList<>();
    private static final Map<String, IObject> objectMap = new HashMap<>();

    /**
     * startup an network.
     *
     * @param context an android context
     * @param network an INetwork instance
     * @return startup successfully or not
     */
    public static boolean startup(Context context, INetwork network) {
        try {
            network.onStartUp(context);
        } catch (Throwable t) {
            return false;
        }
        synchronized (networkList) {
            networkList.add(network);
        }
        return true;
    }

    /**
     * shutdown all networks.
     */
    public static void shutdown() {
        List<INetwork> networks;
        synchronized (networkList) {
            networks = new LinkedList<>(networkList);
            networkList.clear();
        }
        for (INetwork network : networks) {
            try {
                network.onShutDown();
            } catch (Throwable ignore) {
            }
        }
    }

    /**
     * register an object.
     *
     * @param name   unique name of the object
     * @param object an IObject instance
     */
    public static void register(String name, IObject object) {
        synchronized (objectMap) {
            objectMap.put(name, object);
        }
    }

    /**
     * unregister an object.
     *
     * @param name unique name of the object
     */
    public static void unregister(String name) {
        synchronized (objectMap) {
            objectMap.remove(name);
        }
    }

    /**
     * invoke an object method and response the return value.
     *
     * @param requestJson {"xid": 0, "object": "test", "method": "assertTrue", "params": [true]}
     * @return {"xid": 0, "r": null, "e": null}
     */
    public static String doResponse(String requestJson) {
        JSONObject response = new JSONObject();
        response.put("xid", null);
        response.put("r", null);
        response.put("e", null);
        try {
            JSONObject request = JSON.parseObject(requestJson);
            if (request.containsKey("xid")) {
                response.put("xid", request.get("xid"));
            }
            String objectName = request.getString("object");
            String methodName = request.getString("method");
            JSONArray params = request.getJSONArray("params");
            IObject object;
            synchronized (objectMap) {
                object = objectMap.get(objectName);
            }
            if (object == null) {
                throw new NullPointerException("not found '" + objectName + "' object");
            }
            Object[] args;
            Class<?>[] parameterTypes = object.getParameterTypes(methodName);
            if (parameterTypes != null) {
                args = new Object[parameterTypes.length];
                for (int i = 0; i < parameterTypes.length; i++) {
                    args[i] = i < params.size() ? params.getObject(i, parameterTypes[i]) : null;
                }
            } else {
                args = params.toArray();
            }
            response.put("r", object.invoke(methodName, args));
        } catch (Throwable t) {
            String str = t.toString();
            response.put("e", str != null ? str : "");
        }
        return JSONObject.toJSONString(response, SerializerFeature.WriteMapNullValue);
    }
}
