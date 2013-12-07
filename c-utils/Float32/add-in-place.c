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
  int i, j, z;
  int f0_index = 1;
  int f0_fd;
  int f1_fd;
  uint32_t *f0_data;
  uint32_t *f1_data;
  uint32_t file_size;
  uint32_t value_count;
  int status;
  struct stat stat_buf;
  
  if( argc < 2 ) error(1, 0, "Not enough arguments.");
  
  z = stat(argv[f0_index], &stat_buf);
  if( z < 0 ) error(ERROR_NOOP, errno, "Failed to fstat %s", argv[f0_index]);
  
  file_size = (uint32_t)stat_buf.st_size;
  if( file_size & 0x3 ) {
    error(ERROR_NOOP, 0, "%s is not a multiple of 4 bytes (it's %u bytes)", argv[f0_index], file_size );
  }
  value_count = file_size>>2;
  
  f0_fd = open( argv[f0_index], O_RDWR );
  if( f0_fd < 0 ) error(ERROR_NOOP, errno, "Failed to open %s for reading+writing", argv[f0_index]);
  f0_data = mmap( NULL, (uint32_t)file_size, PROT_READ|PROT_WRITE, MAP_SHARED, f0_fd, 0 );
  if( f0_data == MAP_FAILED ) error(ERROR_NOOP, errno, "Failed to mmap %s", argv[f0_index]);
  
  for( i=f0_index+1; i<argc; ++i ) {
    z = stat(argv[i], &stat_buf);
    if( z < 0 ) {
      error(0, errno, "Failed to fstat %s", argv[i]);
      status = ERROR_WITH_UPDATES;
      continue;
    }
    
    file_size = (uint32_t)stat_buf.st_size;
    if( value_count * 4 != file_size ) {
      error(0, errno, "%s is wrong size (%u bytes, expected %u)", argv[i], file_size, value_count * 4);
      status = ERROR_WITH_UPDATES;
    }
    
    f1_fd = open( argv[i], O_RDONLY );
    if( f1_fd < 0 ) {
      error(0, errno, "Failed to open %s for reading", argv[i]);
      status = ERROR_WITH_UPDATES;
      continue;
    }
    f1_data = mmap( NULL, (uint32_t)file_size, PROT_READ, MAP_SHARED, f1_fd, 0 );
    if( f1_data == MAP_FAILED ) {
      error(0, errno, "Failed to mmap %s", argv[i]);
      status = ERROR_WITH_UPDATES;
      if( close(f1_fd) ) {
	error(0, errno, "Failed to close %s", argv[i]);
      }
      continue;
    }
    
    for( j=0; j<value_count; ++j ) {
      f0_data[j] = serf( unserf(f0_data[j]) + unserf(f1_data[j]) );
    }
    
    if( munmap( f1_data, value_count * 4 ) ) {
      error(0, errno, "Failed to munmap %s", argv[i]);
      status = ERROR_WITH_UPDATES;
    }
    if( close( f1_fd ) ) {
      error(0, errno, "Failed to close %s", argv[i]);
      status = ERROR_WITH_UPDATES;
    }
  }
  
  if( munmap( f0_data, value_count * 4 ) ) {
    error(0, errno, "Failed to munmap %s", argv[f0_index]);
    status = ERROR_WITH_UPDATES;
  }
  if( close( f0_fd ) ) {
    error(0, errno, "Failed to close %s", argv[f0_index]);
    status = ERROR_WITH_UPDATES;
  }
  
  return status;
}
