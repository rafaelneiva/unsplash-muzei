package com.rafaelneiva.muzeiunsplash

import android.app.Activity
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import br.com.zup.multistatelayout.MultiStateLayout
import com.rafaelneiva.muzeiunsplash.databinding.ListItemCollectionBinding
import com.rafaelneiva.muzeiunsplash.muzeiunsplash.UnsplashExampleWorker
import com.rafaelneiva.muzeiunsplash.muzeiunsplash.UnsplashService
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SettingsActivity : AppCompatActivity(), CollectionsAdapter.ClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        val adapter = CollectionsAdapter(this)
        rvCollections.adapter = adapter
        rvCollections.layoutManager = LinearLayoutManager(this)
        rvCollections.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        mslContent.setState(MultiStateLayout.State.LOADING)
        UnsplashService.getCollections().observe(this, Observer {
            mslContent.setState(MultiStateLayout.State.CONTENT)
            adapter.submitList(it)
        })
    }

    override fun onClickItem(collection: UnsplashService.Collection) {
        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt(getString(R.string.shpr_collection_id), collection.id).apply()

        GlobalScope.launch {
            UnsplashExampleWorker.enqueueLoad()
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}

class CollectionsAdapter(private val clickListener: ClickListener) : PagedListAdapter<UnsplashService.Collection, CollectionsAdapter.ViewHolder>(DIFF_CALLBACK) {

    interface ClickListener {
        fun onClickItem(collection: UnsplashService.Collection)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        LayoutInflater.from(parent.context).inflate(
            R.layout.list_item_collection,
            parent,
            false
        )
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val collection = getItem(position)
        if (collection != null) {
            holder.bind.collection = collection

            holder.bind.clickableView.setOnClickListener {
                clickListener.onClickItem(collection)
            }
        }
    }

    class ViewHolder(itemView: View, val bind: ListItemCollectionBinding = DataBindingUtil.bind(itemView)!!) : RecyclerView.ViewHolder(itemView)

    companion object {
        private val DIFF_CALLBACK = object :
            DiffUtil.ItemCallback<UnsplashService.Collection>() {
            override fun areItemsTheSame(oldConcert: UnsplashService.Collection, newConcert: UnsplashService.Collection): Boolean = oldConcert.id == newConcert.id

            override fun areContentsTheSame(oldConcert: UnsplashService.Collection, newConcert: UnsplashService.Collection): Boolean = oldConcert == newConcert
        }
    }
}
