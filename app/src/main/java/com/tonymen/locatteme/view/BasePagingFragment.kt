package com.tonymen.locatteme.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.facebook.shimmer.ShimmerFrameLayout
import com.tonymen.locatteme.R

abstract class BasePagingFragment<T, VH : RecyclerView.ViewHolder> : Fragment() {

    protected lateinit var recyclerView: RecyclerView
    protected lateinit var swipeRefreshLayout: SwipeRefreshLayout
    protected lateinit var shimmerFrameLayout: ShimmerFrameLayout
    protected lateinit var pagingAdapter: RecyclerView.Adapter<VH>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_base_paging, container, false)
        recyclerView = view.findViewById(R.id.recyclerView)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        shimmerFrameLayout = view.findViewById(R.id.shimmerFrameLayout)

        setupRecyclerView()
        setupSwipeRefresh()

        return view
    }

    abstract fun setupRecyclerView()

    abstract fun setupSwipeRefresh()

    abstract fun loadData()

    protected fun showLoading() {
        shimmerFrameLayout.startShimmer()
        shimmerFrameLayout.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    protected fun hideLoading() {
        shimmerFrameLayout.stopShimmer()
        shimmerFrameLayout.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }
}
