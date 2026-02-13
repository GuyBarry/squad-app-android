import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.squadapp.R

class GameAdapter(
    context: android.content.Context,
    private val games: List<Game>
) : ArrayAdapter<Game>(context, 0, games) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: android.view.LayoutInflater.from(context).inflate(R.layout.game_dropdown_item, parent, false)
        val game = games[position]

        val gameImage = view.findViewById<ImageView>(R.id.game_image)
        val gameName = view.findViewById<TextView>(R.id.game_name)
        val gamePlatform = view.findViewById<TextView>(R.id.game_platform)

        gameImage.setImageResource(game.imageResId)
        gameName.text = game.name
        gamePlatform.text = game.platform

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getView(position, convertView, parent)
    }
}