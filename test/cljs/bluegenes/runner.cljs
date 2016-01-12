(ns bluegenes.runner
    (:require [doo.runner :refer-macros [doo-tests]]
              [bluegenes.core-test]))

(doo-tests 'bluegenes.core-test)
