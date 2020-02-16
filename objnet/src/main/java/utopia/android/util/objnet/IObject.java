package utopia.android.util.objnet;

public interface IObject {
    Class<?>[] getParameterTypes(String method) throws Throwable;

    Object invoke(String method, Object[] args) throws Throwable;
}
