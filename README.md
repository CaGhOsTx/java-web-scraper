# WebScrapingService
API designed for breadth-first-search web scraping, mainly data collection.
<ul>
  <li> Implemented abstract classes and interfaces to provide users the ability to provide custom 
implementations for HTML parsers, with very little extra code, which parse desired HTML elements and 
collect them. Any amount of parsers can be given to a Web Scraper and each parser is computed in parallel. 
  </li>
  <li>Implemented concurrent processing of get requests using a custom, decoupled thread managing service.
  </li>
  <li>
Implemented a command line run Scraper Service which has the ability to monitor and control multiple 
Web Scrapers at once, stopping individual ones, comparing collection statistics, restarting them, etc
  </li>
</ul>
