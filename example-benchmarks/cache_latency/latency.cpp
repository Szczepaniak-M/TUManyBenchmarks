#include <atomic>
#include <cassert>
#include <iostream>
#include <algorithm>
#include <pthread.h>
#include <cstdint>
#include <sys/time.h>

using namespace std;

static inline double gettime(void) {
   struct timeval now_tv;
   gettimeofday (&now_tv, NULL);
   return ((double)now_tv.tv_sec) + ((double)now_tv.tv_usec)/1000000.0;
}

int main(int argc, char** argv) {
   uint64_t n = atol(argv[1]) / 8;
   unsigned rep = atoi(argv[2]);
   if (!n)
      n = 16;

   uint64_t* v2 = new uint64_t[n];
   for (uint64_t i=0; i<n;i++)
      v2[i] = i;
   random_shuffle(v2,v2+n);

   uint64_t* v = new uint64_t[n];
   for (uint64_t i=0; i<n; i++)
      v[v2[i]] = v2[(i+1)%n];


   uint64_t x = 0, count = 0;

   double start = gettime(), end;
   for (unsigned i=0; i<rep; i++)
      x = v[x];
   end = gettime();
   count += rep;
   cout << (n*8) << "," << (((end-start)*1e9) / count) << endl;
   assert(x+1);

   return 0;
}