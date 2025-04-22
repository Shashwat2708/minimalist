package com.example.minimalistlauncher

import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.minimalistlauncher.databinding.ActivityMainBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val savedApps = mutableListOf<AppInfo>()
    private lateinit var appAdapter: AppAdapter
    private lateinit var timer: Timer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up time and date
        updateTimeAndDate()

        // Set up app list
        setupAppList()

        // Set up add app button
        binding.addAppButton.setOnClickListener {
            showAppSelectionDialog()
        }

        // Set up phone button
        binding.phoneButton.setOnClickListener {
            openDialer()
        }

        // Set up camera button
        binding.cameraButton.setOnClickListener {
            openCamera()
        }
        
        // Start time update timer
        startTimeUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        timer.cancel()
    }

    private fun startTimeUpdates() {
        timer = Timer()
        timer.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                runOnUiThread {
                    updateTimeAndDate()
                }
            }
        }, 0, 60000) // Update every minute
    }

    private fun updateTimeAndDate() {
        val calendar = Calendar.getInstance()
        
        // Update time
        val timeFormat = SimpleDateFormat("H:mm", Locale.getDefault())
        binding.timeTextView.text = timeFormat.format(calendar.time)
        
        // Update date
        val dayOfWeek = SimpleDateFormat("EEEE", Locale.getDefault()).format(calendar.time)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = SimpleDateFormat("MMMM", Locale.getDefault()).format(calendar.time)
        
        val daySuffix = when (day) {
            1, 21, 31 -> "st"
            2, 22 -> "nd"
            3, 23 -> "rd"
            else -> "th"
        }
        
        binding.dateTextView.text = "$dayOfWeek, $day$daySuffix $month"
    }

    private fun setupAppList() {
        appAdapter = AppAdapter(savedApps) { appInfo ->
            val launchIntent = packageManager.getLaunchIntentForPackage(appInfo.packageName)
            if (launchIntent != null) {
                startActivity(launchIntent)
            }
        }
        
        binding.appListRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = appAdapter
        }
        
        // Add some default apps for demonstration
        addDefaultApps()
    }
    
    private fun addDefaultApps() {
        // Add default apps from the screenshot
        val defaultApps = listOf(
            "com.android.dialer" to "Phone",
            "com.android.calendar" to "Calendar",
            "com.android.calculator2" to "Calculator",
            "com.android.mms" to "Messages"
        )
        
        for ((packageName, label) in defaultApps) {
            try {
                val appInfo = AppInfo(label, packageName)
                savedApps.add(appInfo)
            } catch (e: Exception) {
                // App not available on device, skip
            }
        }
        
        appAdapter.notifyDataSetChanged()
    }

    private fun showAppSelectionDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_app_selection, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        
        val allAppsRecyclerView = dialogView.findViewById<RecyclerView>(R.id.allAppsRecyclerView)
        
        // Get all installed apps
        val allApps = getAllInstalledApps()
        
        // Set up recycler view for all apps
        val allAppsAdapter = AppAdapter(allApps) { selectedApp ->
            // Add selected app to the home screen list
            if (!savedApps.any { it.packageName == selectedApp.packageName }) {
                savedApps.add(selectedApp)
                appAdapter.notifyItemInserted(savedApps.size - 1)
            }
            dialog.dismiss()
        }
        
        allAppsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = allAppsAdapter
        }
        
        dialog.show()
    }

    private fun getAllInstalledApps(): List<AppInfo> {
        val mainIntent = Intent(Intent.ACTION_MAIN, null)
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        
        val apps = packageManager.queryIntentActivities(mainIntent, 0)
        
        return apps.map { resolveInfo ->
            AppInfo(
                resolveInfo.loadLabel(packageManager).toString(),
                resolveInfo.activityInfo.packageName
            )
        }.sortedBy { it.appName }
    }

    private fun openDialer() {
        val intent = Intent(Intent.ACTION_DIAL)
        startActivity(intent)
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivity(intent)
    }

    // App adapter for both home screen and app selection
    inner class AppAdapter(
        private val apps: List<AppInfo>,
        private val onItemClick: (AppInfo) -> Unit
    ) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {
        
        inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val appNameTextView: TextView = itemView.findViewById(R.id.appNameTextView)
            
            fun bind(appInfo: AppInfo) {
                appNameTextView.text = appInfo.appName
                
                itemView.setOnClickListener {
                    onItemClick(appInfo)
                }
            }
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app, parent, false)
            return AppViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
            holder.bind(apps[position])
        }
        
        override fun getItemCount() = apps.size
    }
} 