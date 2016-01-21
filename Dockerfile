FROM clojure:onbuild
RUN ["lein", "clean"]
RUN ["lein", "foreign"]
RUN ["lein", "cljsbuild", "once", "min"]
CMD ["lein", "run"]
