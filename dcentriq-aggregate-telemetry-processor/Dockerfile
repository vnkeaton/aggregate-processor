# For building, docker use: 
 	# docker build --build-arg VERSION=0.0.1 --tag dcentriq-notification-processor:0.0.1 .
# To run docker, use:
    # -m 512 => increase the memory
	# --cpu-quota 100000 => 100%
	# docker run -e TARGET_ENV=dev --name dcentriq-notification-processor dcentriq-notification-processor:0.0.1

  FROM eclipse-temurin:17

  ARG JAR_FILE=target/*.jar
  
  ARG TARGET_ENV
  RUN echo $TARGET_ENV
  
  ENV ENVIRONMENT=$TARGET_ENV
  RUN echo $ENVIRONMENT
  
  #copy jar produced by build step
  # For local build to work remove opportunity_service in the prefix
  # of the COPY, ex: target/feat-opportunity....
  COPY ${JAR_FILE} /data/dcentriq-aggregate-telemetry-processor.jar
  
  RUN chmod +x /data/dcentriq-aggregate-telemetry-processor.jar
  RUN ls /data -lai
  
  #run
  ENTRYPOINT ["java", "-Dspring.profiles.active=${ENVIRONMENT}", "-jar", "/data/dcentriq-aggregate-telemetry-processor.jar"]
  