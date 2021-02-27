package mandysax.plus.fragment;
import android.content.res.Configuration;
import android.os.Bundle;

public interface FragmentControllerImpl
{

	public boolean onBackFragment()

	public FragmentController.FragmentController2 getFragmentController2()

	public void resumeFragment()

	public void create(FragmentActivity activity, Bundle activitySavedInstanceState)

	public void saveInstanceState(Bundle outState)

	public void multiWindowModeChanged(boolean isInMultiWindowMode, Configuration newConfig)

	public void multiWindowModeChanged(boolean isInMultiWindowMode)

	public void configurationChanged(Configuration newConfig)

	/*public void buildLifecycle(LifecycleOwner lifecycle)

	 public void destroy()

	 public void clear()*/
}
