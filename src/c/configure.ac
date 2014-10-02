# initial information about the project
AC_INIT([libjxio],[1.0],[])

AC_CONFIG_MACRO_DIR([m4])

# Checks for programs
AC_PROG_LIBTOOL

# check for C compiler and the library compiler
AC_PROG_CC([gcc])
AC_PROG_CXX([g++])

AM_INIT_AUTOMAKE([1.11])
AM_SILENT_RULES([yes])

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

