package com.example.helpers;



public class Send_Helpers {
	private static final String TAG = "Send_Helpers";

	/**
	 * @param fileName  like "DemoPicture.jpg"
	 */
//	static void createExternalStoragePublicPicture(String fileName) {
//	    // Create a path where we will place our picture in the user's
//	    // public pictures directory.  Note that you should be careful about
//	    // what you place here, since the user often manages these files.  For
//	    // pictures and other media owned by the application, consider
//	    // Context.getExternalMediaDir().
//	    File path = Environment.getExternalStoragePublicDirectory(
//	            Environment.DIRECTORY_PICTURES);
//	    File file = new File(path, fileName);
//
//	    try {
//	        // Make sure the Pictures directory exists.
//	        path.mkdirs();
//
//	        // Very simple code to copy a picture from the application's
//	        // resource into the external file.  Note that this code does
//	        // no error checking, and assumes the picture is small (does not
//	        // try to copy it in chunks).  Note that if external storage is
//	        // not currently mounted this will silently fail.
//	        InputStream is = getResources().openRawResource(R.drawable.me);
//	        OutputStream os = new FileOutputStream(file);
//	        byte[] data = new byte[is.available()];
//	        is.read(data);
//	        os.write(data);
//	        is.close();
//	        os.close();
//
//	        // Tell the media scanner about the new file so that it is
//	        // immediately available to the user.
//	        MediaScannerConnection.scanFile(this,
//	                new String[] { file.toString() }, null,
//	                new MediaScannerConnection.OnScanCompletedListener() {
//	            public void onScanCompleted(String path, Uri uri) {
//	                Log.i(TAG, "Scanned " + path + ":");
//	                Log.i(TAG, "-> uri=" + uri);
//	            }
//	        });
//	    } catch (IOException e) {
//	        // Unable to create file, likely because external storage is
//	        // not currently mounted.
//	        Log.w(TAG, "Error writing " + file, e);
//	    }
//	}
//
//	static void deleteExternalStoragePublicPicture() {
//	    // Create a path where we will place our picture in the user's
//	    // public pictures directory and delete the file.  If external
//	    // storage is not currently mounted this will fail.
//	    File path = Environment.getExternalStoragePublicDirectory(
//	            Environment.DIRECTORY_PICTURES);
//	    File file = new File(path, "DemoPicture.jpg");
//	    file.delete();
//	}
//
//	static boolean hasExternalStoragePublicPicture() {
//	    // Create a path where we will place our picture in the user's
//	    // public pictures directory and check if the file exists.  If
//	    // external storage is not currently mounted this will think the
//	    // picture doesn't exist.
//	    File path = Environment.getExternalStoragePublicDirectory(
//	            Environment.DIRECTORY_PICTURES);
//	    File file = new File(path, "DemoPicture.jpg");
//	    return file.exists();
//	}
}
