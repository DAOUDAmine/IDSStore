# Start with OL runtime.
FROM open-liberty

USER root
# Symlink servers directory for easier mounts.
RUN ln -s /opt/ol/wlp/usr/servers /servers
ADD --chown=1001:0 target/liberty/wlp/usr/servers /servers/
RUN chmod -R ug+rX  /servers/esdl-drive-server
ADD --chown=1001:0 target/esdl-drive.war /servers/esdl-drive-server/apps
USER 1001
# copy certificates
RUN mkdir -p /servers/esdl-drive-server/resources/security
COPY certificates/public.p12 /servers/esdl-drive-server/resources/security/
# Run the server script and start the defaultServer by default.
ENTRYPOINT ["/opt/ol/wlp/bin/server", "run"]
CMD ["esdl-drive-server"]
