#! /usr/bin/perl

# collect all the information from the database
open(USERDB, "sort html/users.db|");
while(<USERDB>) {
  chomp;
  ($service, $name, $url, $telnet, $description) = split /[|]/;
  print STDERR "Checking $url ... ";
  if(!system("wget --quiet --spider $url")) {
    print STDERR "OK\n";
    $OUT{$service} .= "<LI><A HREF=\"$url\">$name</A>";
  } else {
    print STDERR "DEFECT\n";
    $OUT{$service} .= "<LI><A HREF=\"$url\">$name</A> [<I>defect?</I>] ";
  }
  print STDERR "Checking $telnet ... ";
  if(!system("wget --quiet --spider $telnet")) {
    print STDERR "OK\n";
    $OUT{$service} .= "(<I><A HREF=\"$telnet\">telnet</A></I>)\n";
  } else {
    print STDERR "DEFECT\n";
    $OUT{$service}.="(<I><A HREF=\"$telnet\">telnet</A></I> [<I>defect?</I>])\n";
  }
}
close(USERDB);
$final = "<UL>\n";
foreach $ser (sort keys %OUT) {
  $final .= "<LI><B>$ser</B>\n";
  $final .= "<UL>\n";
  $final .= $OUT{$ser};
  $final .= "</UL>\n";
}
$final .= "</UL>\n";

open(HTML, "html/users.html");
while(<HTML>) {
  if(/<!-- USER-LIST -->/) {
    print;
    print $final;
  } else {
    print;
  }
}
close(HTML);
