<?

//assumes they are in order of capacity low to high
$a=array(array('h1',1),array('h2',2),array('h3',3));

foreach ($a as $h) {
  for ($i=0; $i<$h[1];$i++) {
    $j=($i%2?1:-1)*ceil($i/2);
    $x[$j][]=$h[0];
    echo $h[0].' : '.$i." : $j \n";
  }
}

print_r($x);
asort($x,SORT_NUMERIC);
print_r($x);

foreach ($x as $h)
  foreach ($h as $h2)
    echo $h2." ";

?>
