package org.evgem.android.bittorrentclient.ui.activity

import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import org.evgem.android.bittorrentclient.R
import org.evgem.android.bittorrentclient.ui.fragment.loadings.LoadingsFragment
import org.evgem.android.bittorrentclient.util.transaction

/**
 * This activity is the only one in the project. Each fragment can safely cast its activity to this.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (savedInstanceState == null) {
            supportFragmentManager.transaction {
                add(R.id.main_fragment_container,
                    LoadingsFragment()
                )
            }
        }
    }

    fun startFragment(fragment: Fragment, addToBackStack: Boolean = false, backStackTag: String? = null) {
        supportFragmentManager.transaction {
            replace(R.id.main_fragment_container, fragment)
            if (addToBackStack) {
                addToBackStack(backStackTag)
            }
        }
    }
}