Automated (junit) tests for mmbase. Things you might like to know.

- Every test should have an entry in build.xml in this directory.

- The run.all target of this build.xml is run every night on mmbase.org

- The config directory defines a mmbase.config directory, including everything which is needed to
  run the tests.

  - an empty mmbase (class path is constructed in directory build/lib) running hsql (the work files
    of hsql are put in a directory 'work')

  - several logging configurations can be chosen (three different log.xml are present now)
     - if test-cases fail you might change logging configuration to explore whats wrong.
     - you might want to require that no warn/error logging are issued. There is log configuration 
       present which converts those into test-case-errors.
	 - this is currently configured in impelmentation of the specific test suites
	 -  
  - three applications are auto-deploy in this test-install:
     - General.xml 
     - BridgeTests.xml (aa, bb, cc builders with all kind of fields)
     - MyNews.xml      (based on MyNews from core, but autodeploy (and perhaps more?))
   