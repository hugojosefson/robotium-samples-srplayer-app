Known limitations and bugs
--------------------------
* Some times there is a problem accessing the RTPS stream using WLAN (WiFi) connection.
Might be related to http://code.google.com/p/android/issues/detail?id=2302


Other information
------------------
Right Now Info - http://api.sr.se/rightnowinfo/RightNowInfoAll.aspx?FilterInfo=true
Info om SR strömmar - http://www.sr.se/sida/artikel.aspx?ProgramID=2321&Artikel=787720

Whishlist
---------
* "Sleep mode" - Ställ in att sluta spela efter en viss tid.

* "Klock radio" - Ställ in så att SR Player börjar spela en kanal vid en viss tid på dagen.

Media Player Error codes
------------------------
-1 is a general failure that usually comes from somewhere in OpenCore.
Unfortunately a lot errors end up as -1 by the time they bubble up.

-4 is PVMFErrNotSupported, which also seems to be a catch-all that
usually occurs during prepare() and probably means that OpenCore found
something it didn't like in the stream.

-38 corresponds
in the source code to 'unknown error'. It seems to appear for a whole
variety of reasons. There's no real way of figuring out what's going on
other than to examine your code and make sure you're doing everything in
the right order. Even then the error appear now and again on stream
start, for me; I catch it and reinitialise the MediaPlayer, and the next
time round it usually works.

-44 means that the video codec cannot keep up. The issue here is that
the OpenCore player engine currently has no mechanism for recovering
if the video codec gets behind. Other frameworks usually drop to an i-
frame only mode until the video codec is back in sync. We expect a fix
for this in a future OpenCore release. In the meantime, you need to be
a bit more conservative about the encoding of your content.

References:
http://www.mail-archive.com/android-developers@googlegroups.com/msg20987.html
http://www.mail-archive.com/android-developers@googlegroups.com/msg14384.html


