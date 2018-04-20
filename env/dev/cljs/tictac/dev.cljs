(ns ^:figwheel-no-load tictac.dev
  (:require
    [tictac.core :as core]
    [devtools.core :as devtools]))

(devtools/install!)

(enable-console-print!)

(core/init!)
