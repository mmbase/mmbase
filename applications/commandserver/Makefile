
.PHONY:
run:
	java  -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8001 -jar `ls target/mmbase-commandserver-*` --maxDuration 1 --threads 1 1679

.PHONY:
test:
	java -cp `ls target/mmbase-commandserver-*` org.mmbase.util.MagickClient Makefile convert -list format
	#java -cp `ls target/mmbase-commandserver-*` org.mmbase.util.MagickClient Makefile sleep 2
