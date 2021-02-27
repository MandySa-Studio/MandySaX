package mandysax.plus.repository;

public interface DataCallback<T>
{
    public void success(T t);
    public void failure(String errorMsg);
}
