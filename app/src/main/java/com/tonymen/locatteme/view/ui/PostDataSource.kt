import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.tonymen.locatteme.model.Post
import kotlinx.coroutines.tasks.await

class PostDataSource : PagingSource<QuerySnapshot, Post>() {

    private val db = FirebaseFirestore.getInstance()

    override suspend fun load(params: LoadParams<QuerySnapshot>): LoadResult<QuerySnapshot, Post> {
        return try {
            val currentPage = params.key ?: db.collection("posts")
                .orderBy("fechaPublicacion", Query.Direction.DESCENDING)
                .limit(params.loadSize.toLong())
                .get()
                .await()

            val lastVisible = currentPage.documents[currentPage.size() - 1]
            val nextPage = db.collection("posts")
                .orderBy("fechaPublicacion", Query.Direction.DESCENDING)
                .startAfter(lastVisible)
                .limit(params.loadSize.toLong())
                .get()
                .await()

            LoadResult.Page(
                data = currentPage.toObjects(Post::class.java).filter { it.estado == "Desaparecido" },
                prevKey = null,
                nextKey = if (nextPage.isEmpty) null else nextPage
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<QuerySnapshot, Post>): QuerySnapshot? {
        return null
    }
}
