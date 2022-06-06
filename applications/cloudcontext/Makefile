
# the maven plugin seems not to be able to do sufficiently
# It'll cost me lots of time to fix the plugin or find work arounds.

# This works well, and is only a few lines.
# Make rocks...

.PHONY: clean


ut:
	java -jar ~/.m2/repository/taglibrarydoc/tlddoc/1.3/tlddoc-1.3.jar   target/mmbase-cloudcontext-1.9-SNAPSHOT.war
	rsync -ra out/* ~/github/mmbase/mmbase.github.io/tags/mm/security


clean:
	rm -r out
