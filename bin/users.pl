#! /usr/bin/perl

# collect all the information from the database
open(USERDB, "sort html/users.db|");
while(<USERDB>) {
  chomp;
  ($service, $name, $url, $telnet, $description) = split /[|]/;
  if(length $url) {
    print STDERR "Checking $url ... ";
    if(length $url && !system("wget --timeout=10 --quiet --spider $url")) {
      print STDERR "OK\n";
      $OUT{$service} .= "<LI><A HREF=\"$url\">$name</A>";
    } else {
      print STDERR "DEFECT\n";
      $OUT{$service} .= "<LI><A HREF=\"$url\">$name</A> [?] ";
    }
  }
  if(length $telnet) {
    print STDERR "Checking $telnet ... ";
    if(length $telnet &&!system("wget --timeout=10 --quiet --spider $telnet")) {
      print STDERR "OK\n";
      $OUT{$service} .= "(<I><A HREF=\"$telnet\">telnet</A></I>)\n";
    } else {
      print STDERR "DEFECT\n";
      $OUT{$service} .=
        "(<I><A HREF=\"$telnet\">telnet</A></I> [?])\n";
    }
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
