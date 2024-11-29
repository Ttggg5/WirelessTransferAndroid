package com.example.wirelesstransferandroid

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.findNavController
import com.example.wirelesstransferandroid.fragments.DisplayScreenFragment

class MainActivity : AppCompatActivity() {
    lateinit var fragmentContainerView: FragmentContainerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        fragmentContainerView = findViewById<FragmentContainerView>(R.id.fragmentContainerView)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        try {
            val fragment = findNavController(R.id.fragmentContainerView).currentDestination
            if(fragment?.id == R.id.displayScreenFragment){
                val dsf = supportFragmentManager.findFragmentById(fragmentContainerView.id)?.childFragmentManager?.fragments[0] as DisplayScreenFragment
                return dsf.onKeyUp(keyCode, event)
            }
            else if (fragment?.id == R.id.fileShareTransferringReceiveFragment) return true
        } catch (ex: Exception) {

        }

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            val fragment = findNavController(R.id.fragmentContainerView).currentDestination
            if(fragment?.id == R.id.homeFragment)
                finish()
            else
                findNavController(R.id.fragmentContainerView).popBackStack()
        }
        return true
    }
}