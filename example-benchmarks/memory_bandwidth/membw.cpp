///usr/bin/env -S g++ "$0" -ltbb -O3 -o /tmp/membw.out && exec /tmp/membw.out "$@"
#include <stdlib.h>    // malloc, free
#include <string.h>    // memset, memcpy
#include <stdint.h>    // integer types
//#include <emmintrin.h> // x86 SSE intrinsics
#include <stdio.h>
#include <assert.h>
#include <sys/time.h>  // gettime
#include <algorithm>   // std::random_shuffle
#include <tbb/tbb.h>
#include <pthread.h>
#include <atomic>
#include <iostream>
#include <sys/mman.h>

#define HUGEPAGE_SIZE (2 * 1024 * 1024) // 2MB per huge page

using namespace tbb;

static double gettime(void) {
   struct timeval now_tv;
   gettimeofday (&now_tv,NULL);
   return ((double)now_tv.tv_sec) + ((double)now_tv.tv_usec)/1000000.0;
}


int main(int argc,char** argv) {
   if (argc < 3) {
      printf("Usage: %s <uint64 count> <threads> <iteration>\n", argv[0]);
      return 1;
   }
   uint64_t n=atof(argv[1]);
   int num_threads = atoi(argv[2]);
   int iterations = atoi(argv[3]);

   // Use Huge Pages
   size_t size_in_bytes = n * sizeof(uint64_t);
   if (size_in_bytes % HUGEPAGE_SIZE != 0) {
     size_in_bytes = ((size_in_bytes / HUGEPAGE_SIZE) + 1) * HUGEPAGE_SIZE;
   }

   uint64_t* keys = (uint64_t*) mmap(nullptr, size_in_bytes,
                                     PROT_READ | PROT_WRITE,
                                     MAP_PRIVATE | MAP_ANONYMOUS | MAP_HUGETLB, -1, 0);

   if (keys == MAP_FAILED) {
       perror("mmap failed");
       return 1;
   }

   oneapi::tbb::global_control global_limit(oneapi::tbb::global_control::max_allowed_parallelism, num_threads);
   static affinity_partitioner ap;

   {
      double start = gettime();
      // Step size 8 to hit cache line only once
      tbb::parallel_for(tbb::blocked_range<uint64_t> (0, n, 8), [&] (const tbb::blocked_range<uint64_t>& range) {
                                                                for (uint64_t i=range.begin(); i!=range.end(); i++) {
                                                                   keys[i] =i;
                                                                }}, ap);
   }

   for (volatile int cnt=0; cnt < iterations; cnt++) {
      std::atomic<uint64_t> s(0);
      double start = gettime();
      tbb::parallel_for(tbb::blocked_range<uint64_t> (0, n), [&] (const tbb::blocked_range<uint64_t>& range) {
                                                                uint64_t sum = 0;
                                                                for (uint64_t i=range.begin(); i!=range.end(); i+=1) {
                                                                   sum += keys[i];
                                                                }
                                                                s += sum;
                                                             }, ap);
      printf("%i,%f,%ld\n",num_threads,(static_cast<double>(sizeof(uint64_t)*n)/(1024ull*1024*1024))/(gettime()-start), s.load());
   }


   return 0;
}
