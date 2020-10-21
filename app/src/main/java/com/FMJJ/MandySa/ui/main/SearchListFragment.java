package com.FMJJ.MandySa.ui.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.media.MediaMetadataCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.FMJJ.MandySa.R;
import com.FMJJ.MandySa.logic.dao.LikeMusicDao;
import com.FMJJ.MandySa.logic.model.contentcatalogs.MusicLibrary;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import mandysax.lifecycle.Fragment;
import mandysax.lifecycle.ViewModelProviders;
import mandysax.lifecycle.livedata.Observer;

public class SearchListFragment extends Fragment
{

    private SearchViewModel viewModel;

	private MusicListAdaper music_listAdaper;

	private EditText search_edit;

	private ImageView search;

	private RecyclerView music_rv;

	private SwipeRefreshLayout music_sl;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container)
	{
		return inflater.inflate(R.layout.search_list_fragment, container, false);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);
		search_edit = view.findViewById(R.id.searchbarEditText1);
		search = view.findViewById(R.id.searchbarImageView1);
		music_rv = view.findViewById(R.id.searchfragmentRecyclerView1);
		music_sl = view.findViewById(R.id.searchfragmentSwipeRefreshLayout1);
	}

	@Override
	public void onFragmentResult(Intent data)
	{
		super.onFragmentResult(data);
		if (!TextUtils.isEmpty(data.getCharSequenceExtra("search_content")))
		{
			music_sl.setRefreshing(true);
			viewModel.song_search(data.getCharSequenceExtra("search_content").toString());
			search_edit.setText(data.getCharSequenceExtra("search_content"));
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		viewModel = ViewModelProviders.of(this).get(SearchViewModel.class);
		search.setOnClickListener(new View.OnClickListener(){

				@Override
				public void onClick(View p1)
				{
					startFragment(SearchFragment.class);
				}	

			});
		search_edit.setFocusable(false);
		search_edit.setLongClickable(false);
		search_edit.setOnClickListener(new View.OnClickListener(){

				@Override
				public void onClick(View p1)
				{
					startFragment(SearchFragment.class);
				}
			});
		LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
		music_rv.setLayoutManager(linearLayoutManager);  
        music_rv.setHasFixedSize(true);
		music_listAdaper = new MusicListAdaper(getContext(), viewModel.song_list);
		music_rv.setAdapter(music_listAdaper);
		music_rv.addOnScrollListener(new RecyclerView.OnScrollListener() {

				@Override
				public void onScrolled(RecyclerView recyclerView, int dx, int dy)
				{
					super.onScrolled(recyclerView, dx, dy);
					LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
					int lastCompletelyVisibleItemPosition = layoutManager.findLastCompletelyVisibleItemPosition();
					if (lastCompletelyVisibleItemPosition == layoutManager.getItemCount() - 1)
					{
						music_sl.setRefreshing(true);			
						viewModel.song_bottom();
					}
				}
			});
        music_sl.setColorScheme(R.color.theme_color);
        music_sl.setOnRefreshListener(
            new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh()
				{
					music_sl.setRefreshing(true);       
					viewModel.song_search(null);
                }
            }
        );
		viewModel.song_search.observeForever(new Observer<List<MediaMetadataCompat>>() {
				@Override
				public void onChanged(List<MediaMetadataCompat> p1)
				{
					if (p1 != null)
					{
						viewModel.song_list.clear();
						viewModel.song_list.addAll(p1);	
						music_listAdaper.notifyDataSetChanged(); 
					}
					else
					{
						Toast.makeText(getContext(), getContext().getString(R.string.error), 10).show();
					}
					music_sl.setRefreshing(false);
				}
			});
		viewModel.song_bottom.observeForever(new Observer<List<MediaMetadataCompat>>() {

				@Override
				public void onChanged(List<MediaMetadataCompat> p1)
				{
					if (p1 != null)
					{
						viewModel.song_list.addAll(p1);	
						music_listAdaper.notifyDataSetChanged();
					}
					else
					{
						Toast.makeText(getContext(), getContext().getString(R.string.error), 10).show();
					}
					music_sl.setRefreshing(false);
				}
			});
	}

	private class MusicListAdaper extends RecyclerView.Adapter<MusicListViewHolder>
	{

		@Override
		public void onBindViewHolder(MusicListViewHolder p1, final int p2)
		{
			final String title=list.get(p2).getString(MediaMetadataCompat.METADATA_KEY_TITLE);
			final String singer=list.get(p2).getString(MediaMetadataCompat.METADATA_KEY_ARTIST);
            p1.Serial_number.setText(p2+1+"");
            setSearchContentColor(title, search_edit.getText().toString(), p1.Song_name);
			setSearchContentColor(singer, search_edit.getText().toString(), p1.Singer_name);
			p1.onclick.setOnClickListener(new View.OnClickListener(){
					@Override
					public void onClick(View p1)
					{
						MusicLibrary.addMusic(list.get(p2));
					}
				});
            p1.onclick.setOnLongClickListener(new View.OnLongClickListener(){

                    @Override
                    public boolean onLongClick(View p1)
                    {
                        // 这里的view代表popupMenu需要依附的view
                        final PopupMenu popupMenu = new PopupMenu(getContext(), p1);
                        // 获取布局文件
                        popupMenu.getMenuInflater().inflate(R.menu.like_menu, popupMenu.getMenu());
                        popupMenu.show();
                        // 通过上面这几行代码，就可以把控件显示出来了
                        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {

                                @Override
                                public boolean onMenuItemClick(MenuItem p1)
                                {             
                                    if (p1.getItemId() == R.id.like)
                                    {
                                        Toast.makeText(getContext(), getContext().getString(R.string.isadded), 10).show();
                                        LikeMusicDao.addMudic(list.get(p2));
                                    }
                                    return false;
                                }

                            });
                        return false;
                    }


                });
		}

		private final List<MediaMetadataCompat> list;

		private final Context context;

		@Override
		public int getItemViewType(int position)
		{
			return position;
		} 

		public MusicListAdaper(Context p0, List<MediaMetadataCompat> p1)
		{ 
			this.context = p0;
			this.list = p1;
		}

		public MusicListViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
		{ 
			final View view = LayoutInflater.from(context).inflate(R.layout.music_item, parent, false); 
			return new MusicListViewHolder(view);
		} 

		public int getItemCount()
		{ 
			return list.size(); 
		} 

		private void setSearchContentColor(String name, String KeyWord, TextView tv)
		{
			final SpannableString s = new SpannableString(name);
			final Matcher m = Pattern.compile(KeyWord).matcher(s);
			while (m.find())
			{
				s.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.search_color)), m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			tv.setText(s);
		}

	} 



}

