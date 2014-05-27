<?php
/* Usage (see also the readme.txt file...):
  cd ~/samknows/skios/apps/libcore/testing
  php -S localhost:8000
  Press Ctrl-C to quit it!

  http://localhost:8000/throttletest.php?result=YES
  ... returns YES ...
  http://localhost:8000/throttletest.php?result=NO
  ... returns NO ...
  http://localhost:8000/throttletest.php?response_code=404
  ... returns the specified response code! ...
*/
$theResponseCode = $_GET["response_code"];
if ($theResponseCode != '') {
  echo 'theResponseCode=(' . $theResponseCode . ')';
  http_response_code($theResponseCode);
  exit(0);
}

// Delays the program execution for the given number of seconds.
sleep(10000);

echo $_GET["result"];
// Dump-out whatever we got from the HTTP headers etc.!
// print_r($_SERVER)
?> 
