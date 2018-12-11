package com.rafaelneiva.muzeiunsplash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rafaelneiva.muzeiunsplash.databinding.ListItemCollectionBinding
import com.rafaelneiva.muzeiunsplash.muzeiunsplash.UnsplashService
import kotlinx.android.synthetic.main.activity_main.*

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        val adapter = CollectionsAdapter()
        rvCollections.adapter = adapter
        rvCollections.layoutManager = LinearLayoutManager(this)
        UnsplashService.getCollections().observe(this, Observer {
            adapter.submitList(it)
        })

    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}

class CollectionsAdapter() : PagedListAdapter<UnsplashService.Collection, CollectionsAdapter.ViewHolder>(DIFF_CALLBACK) {

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
        } else {
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
