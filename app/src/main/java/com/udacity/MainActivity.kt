package com.udacity

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.udacity.util.sendNotification
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : AppCompatActivity() {

    private var downloadID: Long = 0

    private lateinit var customButton: LoadingButton
    private var selectedGitHubRepo: String? = null
    private var selectedGitHubText: String? = null

    private lateinit var notificationManager: NotificationManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var action: NotificationCompat.Action

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))

        customButton = findViewById(R.id.custom_button)
        customButton.setCustomButtonState(ButtonState.Completed)
        customButton.setOnClickListener {
            download()
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {

            //Fetching the download id received with the broadcast
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

            //Checking if the received broadcast is for our enqueued download by matching download id
            if (downloadID == id) {
                val action = intent.action
                if (action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                    val query = DownloadManager.Query()
                    query.setFilterById(intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0));
                    val manager = context!!.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val total: Cursor = manager.query(query)
                    if (total.moveToFirst()) {
                        if (total.count > 0) {
                            val status = total.getInt(total.getColumnIndex(DownloadManager.COLUMN_STATUS))
                            /** Call sendNotification */
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                customButton.setCustomButtonState(ButtonState.Completed)
                                notificationManager.sendNotification(selectedGitHubText.toString(), applicationContext, "Success")
                            } else {
                                customButton.setCustomButtonState(ButtonState.Completed)
                                notificationManager.sendNotification(selectedGitHubRepo.toString(), applicationContext, "Failed")
                            }
                        }
                    }
                }
            }

        }
    }

    private fun download() {
        customButton.setCustomButtonState(ButtonState.Clicked)

        if (selectedGitHubRepo != null) {
            customButton.setCustomButtonState(ButtonState.Loading)
            /** Get an instance of NotificationManager*/
            notificationManager = ContextCompat.getSystemService(applicationContext, NotificationManager::class.java) as NotificationManager
            /** Call to create channel */
            createChannel(
                getString(R.string.github_repo_channel_id),
                getString(R.string.github_repo_channel_name)
            )

            val file = File(getExternalFilesDir(null), "/repos")
            if (!file.exists()) { file.mkdirs() }
            val request =
                DownloadManager.Request(Uri.parse(selectedGitHubRepo))
                    // Title of the Download Notification
                    .setTitle(getString(R.string.app_name))
                    // Description of the Download Notification
                    .setDescription(getString(R.string.app_description))
                    // Set if charging is required to begin the download
                    .setRequiresCharging(false)
                    // Set if download is allowed on Mobile network
                    .setAllowedOverMetered(true)
                    // Set if download is allowed on roaming network
                    .setAllowedOverRoaming(true)
                    // Uri of the destination files directory
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "/repos/repository.zip")

            val downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
            downloadID =
                downloadManager.enqueue(request)// enqueue puts the download request in the queue.

        } else{
            customButton.setCustomButtonState(ButtonState.Completed)
            // Toast when is not a file selected
            showToast(getString(R.string.no_file_selected))
        }

    }

    private fun showToast(string: String) {
        val toast = Toast.makeText(this, string, Toast.LENGTH_SHORT)
        toast.show()
    }

    companion object {
        private const val URL =
            "https://github.com/udacity/nd940-c3-advanced-android-programming-project-starter/archive/master.zip"
        private const val CHANNEL_ID = "channelId"
    }


    // When radio button are checked
    fun onRadioButtonChecked(view: View) {
        if (view is RadioButton) {
            val isChecked = view.isChecked
            when (view.getId()) {
                R.id.glide_button ->
                    if (isChecked) {
                        selectedGitHubRepo = getString(R.string.glide_repo_url)
                        selectedGitHubText = getString(R.string.glide_text)
                    }

                R.id.loadApp_button ->
                    if (isChecked) {
                        selectedGitHubRepo = getString(R.string.load_app_repo_url)
                        selectedGitHubText = getString(R.string.load_app_text)
                    }

                R.id.retrofit_button -> {
                    if (isChecked) {
                        selectedGitHubRepo = getString(R.string.retrofit_repo_url)
                        selectedGitHubText = getString(R.string.retrofit_text)
                    }
                }
            }
        }
    }

    private fun createChannel(channelId: String, channelName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
                //disable badges for this channel
                .apply {setShowBadge(false)}

            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
            notificationChannel.description = "Download is Complete"

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

}
