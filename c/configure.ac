# this is example-file: configure.ac

# initial information about the project
AC_INIT([libjx],[1.0],[])



# check if the source folder is correct

# Checks for programs
AC_PROG_RANLIB

# store current user given compiler flags to avoid default setup via AC_PROG_CC
OLD_CFLAGS=$CFLAGS

# check for C compiler and the library compiler
AC_PROG_CC([gcc])
AC_PROG_CXX([g++])

# reset compiler flags to initial flags
CFLAGS=$OLD_CFLAGS

AM_INIT_AUTOMAKE([1.9])

# use the C compiler for the following checks
AC_LANG([C],[C++])

# --> set these variables instead of CFLAGS or LDFLAGS
AC_SUBST([AM_CFLAGS])
AC_SUBST([AM_LDFLAGS])
AC_SUBST([LIBS])

# files to generate via autotools (.am or .in source files)
AC_CONFIG_FILES([Makefile])
AC_CONFIG_FILES([src/Makefile])

AC_OUTPUT

