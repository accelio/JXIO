#! /bin/sh

libtoolize \
&& aclocal \
&& automake --gnu --add-missing \
&& autoconf
