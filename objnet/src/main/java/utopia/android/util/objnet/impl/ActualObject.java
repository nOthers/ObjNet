package utopia.android.util.objnet.impl;

import utopia.android.util.objnet.IObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ActualObject implements IObject {
    private Object mObject;

    public ActualObject(Object object) {
        mObject = object;
    }

    private Method getMethod(String name) throws NoSuchMethodException {
        Class<?> objectClass = mObject.getClass();
        while (objectClass != null) {
            for (Method method : objectClass.getDeclaredMethods()) {
                if (method.getName().equals(name)) {
                    return method;
                }
            }
            objectClass = objectClass.getSuperclass();
        }
        throw new NoSuchMethodException("not found '" + name + "' method");
    }

    @Override
    public Object invoke(String method, Object[] args) throws Throwable {
        Method m = getMethod(method);
        m.setAccessible(true);
        try {
            return m.invoke(mObject, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Override
    public Class<?>[] getParameterTypes(String method) throws Throwable {
        return getMethod(method).getParameterTypes();
    }
}
