# Level Cache
add 2-level cache for spring boot project.  
modify/rewrite springboot-cache to support 2 level cache 

1st level - caffeine  
2ne level - redis  

## Cache 
- Cache Container Structure
    - ConcurrentMap<String name,ConcurrentMap<String cacheKey,Cache cache> cacheMap>
   
- Cache 

- Cache Setting