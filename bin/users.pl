#! /usr/bin/perl

# collect all the information from the database
open(USERDB, "sort html/users.db|");
while(<USERDB>) {
  chomp;
  ($service, $name, $url, $telnet, $description) = split /[|]/;
  $OUT{$service} .=
    "<LI><A HREF=\"$url\">$name</A> (<I><A HREF=\"$telnet\">telnet</A></I>)\n";
}
print "<UL>\n";
foreach $ser (sort keys %OUT) {
  print "<LI><B>$ser</B>\n";
  print "<UL>\n";
  print $OUT{$ser};
  print "</UL>\n";
}
print "</UL>\n";
