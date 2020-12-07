package com.example.hakonsreader.dialogadapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.example.hakonsreader.R

/**
 * Adapter to display a list of OAuth scopes and its explanation
 */
class OAuthScopeAdapter(
        context: Context,
        resourceId: Int,
        private val scopes: ArrayList<String>
) : ArrayAdapter<String>(context, resourceId, scopes) {


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getCustomView(position, parent)
    }

    private fun getCustomView(position: Int, viewGroup: ViewGroup) : View {
        val view = LayoutInflater.from(context).inflate(R.layout.list_item_oauth_explanation, viewGroup, false)

        val header = view.findViewById<TextView>(R.id.oauthExplanationHeader)
        val content = view.findViewById<TextView>(R.id.oauthExplanationContent)

        val scope = scopes[position]

        header.text = "$scope - "
        content.text = getOAuthExplanation(scope)

        return view
    }

    /**
     * Retrieves the explanation for an OAuth scope
     *
     * @param scope The scope to retrieve the explanation for. Must match exactly with how it is sent to reddit
     * @return A string, or an empty string if the scope wasn't found
     */
    private fun getOAuthExplanation(scope: String?): String? {
        val stringRes = when (scope) {
            "identity" -> R.string.oauthScopeExplanationIdentity
            "account" -> R.string.oauthScopeExplanationAccount
            "history" -> R.string.oauthScopeExplanationHistory
            "mysubreddits" -> R.string.oauthScopeExplanationMysubreddits
            "vote" -> R.string.oauthScopeExplanationVote
            "subscribe" -> R.string.oauthScopeExplanationSubscribe
            "submit" -> R.string.oauthScopeExplanationSave
            "save" -> R.string.oauthScopeExplanationSave
            "edit" -> R.string.oauthScopeExplanationEdit
            "flair" -> R.string.oauthScopeExplanationFlair
            "privatemessages" -> R.string.oauthScopeExplanationPrivatemessage
            "modposts" -> R.string.oauthScopeExplanationModposts
            else -> -1
        }

        return if (stringRes == -1) {
            ""
        } else {
            context.getString(stringRes)
        }
    }
}