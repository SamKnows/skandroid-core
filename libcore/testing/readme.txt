1) In Terminal (change the path to suit, of course...):

  cd ~/samknows/skandroid/apps/libcore/testing
  php -S 0.0.0.0:8000

  Press Ctrl-C to quit it!


2) Then, run the Emulator. This will look here:

  http://10.0.2.2:8000/throttletest.php?YES

  <returns YES>
  
  http://10.0.2.2:8000/throttletest.php?result=YES
  ... returns YES ...
  http://10.0.2.2:8000/throttletest.php?result=NO
  ... returns NO ...
  http://10.0.2.2:8000/throttletest.php?response_code=404
  ... returns the above response code! ...
