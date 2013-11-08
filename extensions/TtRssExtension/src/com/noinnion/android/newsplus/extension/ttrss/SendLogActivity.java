package com.noinnion.android.newsplus.extension.ttrss;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import com.noinnion.android.newsplus.extension.ttrss.utils.AndroidUtils;
import com.noinnion.android.newsplus.extension.ttrss.utils.Utils;

public class SendLogActivity extends Activity {
	
	public final static String	TAG						= "SendLogActivity";												//$NON-NLS-1$

	public static final String	APP_PACKAGE				= "com.noinnion.android.newsplus.extension.google_reader";			//$NON-NLS-1$
	public static final String	APP_EMAIL				= "noinnion@gmail.com";												//$NON-NLS-1$

	// directories
	public static final String	APP_DIRECTORY			= "/NewsPlus/";
	public static final String	APP_PATH				= Environment.getExternalStorageDirectory() + "/" + APP_DIRECTORY;

	public static final String	LOG_DIRECTORY			= APP_DIRECTORY + ".log/";
	public static final String	LOG_PATH				= Environment.getExternalStorageDirectory() + "/" + LOG_DIRECTORY;

	public static final String	EXTRA_SEND_LOG			= APP_PACKAGE + ".extra.SEND_LOG";									//$NON-NLS-1$
	public static final String	EXTRA_FEATURE_REQUEST	= APP_PACKAGE + ".extra.FEATURE_REQUEST";							//$NON-NLS-1$
	public static final String	EXTRA_BUG_REPORT 		= APP_PACKAGE + ".extra.BUG_REPORT";//$NON-NLS-1$

	final int					MAX_LOG_MESSAGE_LENGTH	= 100000;

	public final static String	LINE_SEPARATOR			= System.getProperty("line.separator");	//$NON-NLS-1$

	private AlertDialog			mMainDialog;
	private Intent				mSendIntent;
	private CollectLogTask		mCollectLogTask;
	private ProgressDialog		mProgressDialog;
	private String				mAdditonalInfo;
	private String[]			mFilterSpecs;
	private String				mFormat;
	private String				mBuffer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mSendIntent = null;

		boolean extraSendLog = false;
		boolean extraFeatureRequest = false;
		boolean extraBugReport = false;

		Intent intent = getIntent();
		if (null != intent){
			extraSendLog = intent.getBooleanExtra(EXTRA_SEND_LOG, false);
			extraFeatureRequest = intent.getBooleanExtra(EXTRA_FEATURE_REQUEST, false);
			extraBugReport = intent.getBooleanExtra(EXTRA_BUG_REPORT, false);
		}

		// init
		mFilterSpecs = new String[] { "*:W" };

		if (null == mSendIntent) {
			// standalone application
			mSendIntent = new Intent(Intent.ACTION_SEND);
			mSendIntent.setType("text/plain");//$NON-NLS-1$
			mSendIntent.putExtra(Intent.EXTRA_EMAIL, new String[] { APP_EMAIL });

			String subject = getString(R.string.log_feedback_subject);

			String version = AndroidUtils.getVersionName(this);
			version = (version == null ? "?" : version);

			subject += " " + getText(R.string.app_name) + " (" + version + ")";

			mSendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);

			MessageFormat infoFormat = new MessageFormat(getText(R.string.log_device_info_fmt).toString());
			mAdditonalInfo = "";
			mAdditonalInfo += "\n\n";
			mAdditonalInfo += infoFormat.format(new String[] { version , Build.MODEL, Build.VERSION.RELEASE, getFormattedKernelVersion(), Build.DISPLAY });
//            mAdditonalInfo = getString(R.string.log_device_info_fmt, getVersionNumber(this), Build.MODEL, Build.VERSION.RELEASE, getFormattedKernelVersion(), Build.DISPLAY);

			mFormat = "time";
		}

		if (extraSendLog) {
			mMainDialog = new AlertDialog.Builder(this).setTitle(getString(R.string.log_send_feedback))
				.setMessage(getString(R.string.log_main_dialog_text))
				.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						collectAndSendLog();
					}
				}).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int whichButton) {
						finish();
					}
				}).show();
		}
		else {
			mSendIntent.putExtra(Intent.EXTRA_TEXT, mAdditonalInfo);
			startActivity(Intent.createChooser(mSendIntent, getString(R.string.log_chooser_title)));
			finish();
		}
	}

	@SuppressWarnings("unchecked")
	void collectAndSendLog() {
		/*
		 * Usage: logcat [options] [filterspecs]
		 * options include:
		 * -s Set default filter to silent.
		 * Like specifying filterspec '*:s'
		 * -f <filename> Log to file. Default to stdout
		 * -r [<kbytes>] Rotate log every kbytes. (16 if unspecified). Requires -f
		 * -n <count> Sets max number of rotated logs to <count>, default 4
		 * -v <format> Sets the log print format, where <format> is one of:
		 * brief process tag thread raw time threadtime long
		 * -c clear (flush) the entire log and exit
		 * -d dump the log and then exit (don't block)
		 * -g get the size of the log's ring buffer and exit
		 * -b <buffer> request alternate ring buffer
		 * ('main' (default), 'radio', 'events')
		 * -B output the log in binary
		 * filterspecs are a series of
		 * <tag>[:priority]
		 * where <tag> is a log component tag (or * for all) and priority is:
		 * V Verbose
		 * D Debug
		 * I Info
		 * W Warn
		 * E Error
		 * F Fatal
		 * S Silent (supress all output)
		 * '*' means '*:d' and <tag> by itself means <tag>:v
		 * If not specified on the commandline, filterspec is set from ANDROID_LOG_TAGS.
		 * If no filterspec is found, filter defaults to '*:I'
		 * If not specified with -v, format is set from ANDROID_PRINTF_LOG
		 * or defaults to "brief"
		 */

		ArrayList<String> list = new ArrayList<String>();

		if (mFormat != null) {
			list.add("-v");
			list.add(mFormat);
		}

		if (mBuffer != null) {
			list.add("-b");
			list.add(mBuffer);
		}

		if (mFilterSpecs != null) {
			for (String filterSpec : mFilterSpecs) {
				list.add(filterSpec);
			}
		}

		mCollectLogTask = (CollectLogTask) new CollectLogTask().execute(list);
	}

	private class CollectLogTask extends AsyncTask<ArrayList<String>, Void, StringBuilder> {
		@Override
		protected void onPreExecute() {
			showProgressDialog(getString(R.string.log_acquiring_log_progress_dialog_message));
		}

		@Override
		protected StringBuilder doInBackground(ArrayList<String>... params) {
			final StringBuilder log = new StringBuilder();
			try {
				ArrayList<String> commandLine = new ArrayList<String>();
				commandLine.add("logcat");//$NON-NLS-1$
				commandLine.add("-d");//$NON-NLS-1$
				ArrayList<String> arguments = ((params != null) && (params.length > 0)) ? params[0] : null;
				if (null != arguments) {
					commandLine.addAll(arguments);
				}

				Process process = Runtime.getRuntime().exec(commandLine.toArray(new String[0]));
				BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

				String line;
				while ((line = bufferedReader.readLine()) != null) {
					log.append(line);
					log.append(LINE_SEPARATOR);
				}
			} catch (IOException e) {
				Log.e(TAG, "CollectLogTask.doInBackground failed", e);//$NON-NLS-1$
			}

			return log;
		}

		@Override
		protected void onPostExecute(StringBuilder log) {
			if (null != log) {
				// truncate if necessary
				int keepOffset = Math.max(log.length() - MAX_LOG_MESSAGE_LENGTH, 0);
				if (keepOffset > 0){
					log.delete(0, keepOffset);
				}

				if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
					if (mAdditonalInfo != null) {
						log.insert(0, LINE_SEPARATOR);
						log.insert(0, mAdditonalInfo);
					}

					mSendIntent.putExtra(Intent.EXTRA_TEXT, log.toString());
				} else {
					String fn = "log_" + System.currentTimeMillis() + ".txt";
					try {
						Utils.writeContentToSD(LOG_DIRECTORY, fn, log.toString());

						if (mAdditonalInfo != null) mSendIntent.putExtra(Intent.EXTRA_TEXT, mAdditonalInfo);
						mSendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + LOG_PATH + fn));

					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				startActivity(Intent.createChooser(mSendIntent, getString(R.string.log_chooser_title)));
				dismissProgressDialog();
				dismissMainDialog();
				finish();
			} else {
				dismissProgressDialog();
				showErrorDialog(getString(R.string.log_failed_to_get_log_message));
			}
		}
	}

	void showErrorDialog(String errorMessage) {
		new AlertDialog.Builder(this).setTitle(getString(R.string.app_name)).setMessage(errorMessage).setIcon(android.R.drawable.ic_dialog_alert).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				finish();
			}
		}).show();
	}

	void dismissMainDialog() {
		if (null != mMainDialog && mMainDialog.isShowing()) {
			mMainDialog.dismiss();
			mMainDialog = null;
		}
	}

	void showProgressDialog(String message) {
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setIndeterminate(true);
		mProgressDialog.setMessage(message);
		mProgressDialog.setCancelable(true);
		mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				cancellCollectTask();
				finish();
			}
		});
		mProgressDialog.show();
	}

	private void dismissProgressDialog() {
		if (null != mProgressDialog && mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
			mProgressDialog = null;
		}
	}

	void cancellCollectTask() {
		if (mCollectLogTask != null && mCollectLogTask.getStatus() == AsyncTask.Status.RUNNING) {
			mCollectLogTask.cancel(true);
			mCollectLogTask = null;
		}
	}

	@Override
	protected void onPause() {
		cancellCollectTask();
		dismissProgressDialog();
		dismissMainDialog();

		super.onPause();
	}

	private String getFormattedKernelVersion() {
		String procVersionStr;

		try {
			BufferedReader reader = new BufferedReader(new FileReader("/proc/version"), 256);
			try {
				procVersionStr = reader.readLine();
			} finally {
				reader.close();
			}

			final String PROC_VERSION_REGEX = "\\w+\\s+" + /* ignore: Linux */
					"\\w+\\s+" + /* ignore: version */
					"([^\\s]+)\\s+" + /* group 1: 2.6.22-omap1 */
					"\\(([^\\s@]+(?:@[^\\s.]+)?)[^)]*\\)\\s+" + /* group 2: (xxxxxx@xxxxx.constant) */
					"\\([^)]+\\)\\s+" + /* ignore: (gcc ..) */
					"([^\\s]+)\\s+" + /* group 3: #26 */
					"(?:PREEMPT\\s+)?" + /* ignore: PREEMPT (optional) */
					"(.+)"; /* group 4: date */

			Pattern p = Pattern.compile(PROC_VERSION_REGEX);
			Matcher m = p.matcher(procVersionStr);

			if (!m.matches()) {
				Log.e(TAG, "Regex did not match on /proc/version: " + procVersionStr);
				return "Unavailable";
			} else if (m.groupCount() < 4) {
				Log.e(TAG, "Regex match on /proc/version only returned " + m.groupCount() + " groups");
				return "Unavailable";
			} else {
				return (new StringBuilder(m.group(1)).append("\n").append(m.group(2)).append(" ").append(m.group(3)).append("\n").append(m.group(4))).toString();
			}
		} catch (IOException e) {
			Log.e(TAG, "IO Exception when getting kernel version for Device Info screen", e);

			return "Unavailable";
		}
	}
}