#include <errno.h>
#include <fcntl.h>
#include <stdint.h>
#include <stdio.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>

float unserf( uint32_t ival ) {
  ival = ntohl(ival);
  return *((float *)&ival);
}
uint32_t serf( float fval ) {
  return htonl(*((uint32_t *)&fval));
}

const int ERROR_NOOP = 1;
const int ERROR_WITH_UPDATES = 2;

int main( int argc, char **argv ) {
  float f;
  uint32_t e;
  int l = 0;
  
  while( fread(&e, 4, 1, stdin) ) {
    if( l ) putchar(' ');
    l += printf( "%f", unserf(e) );
    if( l > 70 ) {
      putchar('\n');
      l = 0;
    }
  }
  if( l ) putchar('\n');
}
