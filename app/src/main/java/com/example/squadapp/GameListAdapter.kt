import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.squadapp.R

class GameListAdapter(
    private val games: List<Game>,
    private val onGameSelected: (Game) -> Unit
) : RecyclerView.Adapter<GameListAdapter.GameViewHolder>() {

    inner class GameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val gameImage: ImageView = itemView.findViewById(R.id.game_image)
        private val gameName: TextView = itemView.findViewById(R.id.game_name)
        private val gamePlatform: TextView = itemView.findViewById(R.id.game_platform)

        fun bind(game: Game) {
            gameImage.setImageResource(game.imageResId)
            gameName.text = game.name
            gamePlatform.text = game.platform
            itemView.setOnClickListener {
                onGameSelected(game)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GameViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.game_dropdown_item, parent, false)
        return GameViewHolder(view)
    }

    override fun onBindViewHolder(holder: GameViewHolder, position: Int) {
        holder.bind(games[position])
    }

    override fun getItemCount(): Int = games.size
}

