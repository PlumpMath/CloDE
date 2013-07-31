;reference wpf assemblys

(ns wpf
 (:import
   [System.Collections ICollection]
   [System.ComponentModel PropertyDescriptor MemberDescriptor 
    TypeConverterAttribute TypeConverter]
   [System.IO File] 
   [System.Reflection BindingFlags PropertyInfo MethodInfo EventInfo]
   [System.Threading ApartmentState ParameterizedThreadStart Thread ThreadStart
    EventWaitHandle EventResetMode]
   [System.Windows Application Window EventManager DependencyObject DependencyProperty
    FrameworkPropertyMetadata LogicalTreeHelper ResourceDictionary]
   [System.Windows.Data BindingBase Binding BindingOperations]
   [System.Windows.Input CommandBinding ExecutedRoutedEventHandler
    CanExecuteRoutedEventHandler]
   [System.Windows.Markup XamlReader]
   [System.Windows.Threading Dispatcher DispatcherObject DispatcherPriority
    DispatcherUnhandledExceptionEventHandler]
   [System.Xaml XamlSchemaContext XamlType]
   [System.Xaml.Schema XamlTypeName]
   [System.Xml XmlReader]
   [Common.Logging]
   )
  (:require
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    )
  ;  (:require [clojure.tools.cli :as c])
)

; (import [System.Reflection Assembly])
; (defn load-resource [filename]
;   (let [assembly-path (-> Assembly/GetExecutingAssembly 
;                         GetName 
;                         CodeBase)
;         folder-path (Path/GetDirectoryName assembly-path)
;         resource-uri (-> Uri. (Path/Combine folder-path filename) .get_LocalPath)
;         text (File/ReadAllText resource-uri) 
;         resource-dict (XamlReader/Parse text) ]
;     ))

; (defn default-main [& [fileName]]
;   ;    (let [[options args banner]
;   ;          (c/cli args
;   ;                 ["-x" "--xaml" "xaml file to load with window as root" :default "MainWindow.xaml"])]
;   (let [app (Application.)]
;     (reset! win (if-not (string/blank? fileName)
;                   (-> fileName
;                       wpf/load-xaml)
;                   (Window.)))
;     (. app Run @win)))

; (defn -main [& args]
;   (let [uithread (create-sta-thread (apply partial default-main args))] 
;     (clojure.main/repl :eval (partial wpf-eval uithread (ns-name *ns*)))))

(defn load-xaml [filename]
  (-> filename File/OpenText XmlReader/Create XamlReader/Load))

(defn load-resource [uri]
  (let [dict (ResourceDictionary.)]
    (set! (.Source dict) (Uri. uri))
    dict))

(defn merge-app-resource [dict]
  (.. Application/Current Resources MergedDictionaries (Add dict)))

(defn create-sta-thread [func]
  (let [delegate (gen-delegate ThreadStart [] (func))
        thread (doto (new Thread delegate)
                 (.SetApartmentState ApartmentState/STA)
                 (.Start))]
    thread))

(defn wpf-eval
  [uithread repl-ns-symbol data]
  (let [wpf-result (atom nil)]
    (.Invoke (Dispatcher/FromThread uithread) DispatcherPriority/Normal
             (gen-delegate Action []
                           (clojure.main/with-bindings
                             (in-ns repl-ns-symbol)
                             (try (reset! wpf-result (eval data))
                               (catch Exception e (prn e)))
                             ))) 
    @wpf-result))

(defn run
  ([main ns-symbol]
   (let [uithread (create-sta-thread (apply partial main *command-line-args*))]
     (log/info "starting repl")
     (clojure.main/repl :eval (partial wpf-eval uithread ns-symbol))))
  ([main ns-symbol & args]
   (let [uithread (create-sta-thread (apply partial main args))]
     (clojure.main/repl :eval (partial wpf-eval uithread ns-symbol))))
  )

(defn get-app-main-window []
  (when-let [app Application/Current]
    (.MainWindow app)))


; (def ^:dynamic *cur* nil)

; (def ^:private default-xaml-ns {:ns "http://schemas.microsoft.com/winfx/2006/xaml/presentation"
;                                 :context (XamlReader/GetWpfSchemaContext)})

; (def ^:private default-xaml-ns-x {:ns "http://schemas.microsoft.com/winfx/2006/xaml"
;                                   :context (XamlReader/GetWpfSchemaContext)})

; (def default-xaml-context
;   {nil default-xaml-ns :x default-xaml-ns-x})

; (def ^:dynamic *xaml-schema-ctxt* default-xaml-context)

; (defn resolve-xaml-type [xaml-ctxt nexpr]
;   (let [nparts (str/split nexpr #":")
;         is-split (> (count nparts) 1)
;         tname (if is-split (second nparts) (first nparts))
;         nsname (when is-split (first nparts))
;         ns-ctxt (get xaml-ctxt (keyword nsname))
;         nsname (:ns ns-ctxt)
;         ctxt (:context ns-ctxt)
;         xaml-name (XamlTypeName. nsname tname)]
;     (.GetXamlType ctxt xaml-name)))

; (defn with-invoke* [^DispatcherObject dispatcher-obj func]
;   (let [dispatcher (.get_Dispatcher dispatcher-obj)]
;     (if (.CheckAccess dispatcher)
;       (func)
;       (.Invoke dispatcher DispatcherPriority/Normal
;                (sys-func [Object] [] (func))))))

; (defmacro with-invoke [dispatcher-obj & body]
;   `(wpf/with-invoke* ~dispatcher-obj (fn [] ~@body)))

; (defn with-begin-invoke* [^DispatcherObject dispatcher-obj func]
;   (let [dispatcher (.get_Dispatcher dispatcher-obj)]
;     (if (.CheckAccess dispatcher)
;       (func)
;       (.BeginInvoke dispatcher DispatcherPriority/Normal
;                     (sys-func [Object] [] (func))))))

; (defmacro with-begin-invoke [dispatcher-obj & body]
;   `(wpf/with-begin-invoke* ~dispatcher-obj (fn [] ~@body)))

; (defn find-elem [target path] (reduce #(LogicalTreeHelper/FindLogicalNode % (name %2)) target path))

; (defn find-elem-warn [target path]
;   (or (find-elem target path) (println "Unable to find " path " in " target)))

; (defn compile-target-expr [target]
;   (let [path? (vector? target)
;         target? (when path? (first target))
;         implicit-target? (when path? (keyword? target?))
;         dispatcher-obj (if path?
;                          (if implicit-target? `wpf/*cur* target?)
;                          target)
;         path-expr (when path? (if implicit-target? (vec target) (vec (rest target))))
;         target (if path?
;                  `(wpf/find-elem-warn ~dispatcher-obj ~path-expr)
;                  target)]
;     [dispatcher-obj target]))

; (defmacro doat [target & body]
;   (let [[dispatcher-obj target] (compile-target-expr target)]
;     `(wpf/with-invoke ~dispatcher-obj
;        (clojure.core/binding [wpf/*cur* ~target]
;                              ~@body))))

; (defmacro async-doat [target & body]
;   (let [[dispatcher-obj target] (compile-target-expr target)]
;     `(wpf/with-begin-invoke ~dispatcher-obj
;        (clojure.core/binding [wpf/*cur* ~target]
;                              ~@body))))

; (def *dispatcher-exception (atom nil))

; (defn dispatcher-unhandled-exception [sender args]
;   (let [ex (.get_Exception args)]
;     (reset! *dispatcher-exception ex)
;     (println "Dispatcher Exception: " ex)
;     (log/error ex "Dispatcher Exception")
;     (.set_Handled args true)))


; (defn default-init []
;   (let [window (Window.)]
;     (.set_Title window "Window")
;     (.Show window)
;     window))

; (defn separate-threaded-window
;   [& {:as opts}]
;   (let [{:keys [exception-handler title show init]} (merge {:title "Window"
;                                                        :show true
;                                                        :init default-init
;                                                        :exception-handler dispatcher-unhandled-exception} opts)
;         window (atom nil)
;         waitHandle (EventWaitHandle. false EventResetMode/AutoReset)
;         thread (doto (Thread.
;                       (gen-delegate ParameterizedThreadStart [window]
;                                     (reset! window (init))
;                                     (.add_UnhandledException Dispatcher/CurrentDispatcher
;                                                              (gen-delegate DispatcherUnhandledExceptionEventHandler [s e]
;                                                                            (log/trace "trying to dispatch exception" s e)
;                                                                            (exception-handler s e)))
;                                     (.Set waitHandle)
;                                     (Dispatcher/Run)))
;                  (.SetApartmentState ApartmentState/STA)
;                  (.Start window))]
;     (.WaitOne waitHandle)
;     {:thread thread :window @window}))

; (defn app-start [application-class]
;   (doto (Thread.
;          (gen-delegate ThreadStart [] (.Run (Activator/CreateInstance application-class))))
;     (.SetApartmentState ApartmentState/STA)
;     (.Start)))

; (def ^:private xamlClassRegex #"x:Class=\"[\w\.]+\"")

; (defn load-dev-xaml [path]
;   (let [xaml (slurp path :econding "UTF8")
;         xaml (.Replace xamlClassRegex xaml "")]
;     (XamlReader/Parse xaml)))

; (def ^:dynamic *dev-mode* false)

; (defn xaml-view
;   ([constructor dev-xaml-path]
;      (xaml-view constructor identity dev-xaml-path))
;   ([constructor
;     mutator
;     dev-xaml-path]
;      (fn [] (let [view (if (and *dev-mode* dev-xaml-path (File/Exists dev-xaml-path))
;                          (load-dev-xaml dev-xaml-path) (constructor))]
;               (mutator view)
;               view))))

; (defprotocol IAttachedData (attach [this target value]))

; (defrecord ^:private AttachedData [^DependencyProperty prop]
;            IAttachedData
;            (attach [this target value] (wpf/with-invoke target (.SetValue target prop value)))
;            clojure.lang.IDeref
;            (deref [this] (when *cur* (.GetValue *cur* prop)))
;            clojure.lang.IFn
;            (invoke [this target] (.GetValue target prop)))

; (defmethod print-method AttachedData [x writer]
;   (.Write writer "#<AttachedData ")
;   (print-method (:prop x) writer)
;   (.Write writer ">"))

; (defn create-attached-data [^DependencyProperty prop] (AttachedData. prop))

; (defn event-dg-helper [target evt-method-info handler]
;   (let [dg (if-not (instance? Delegate handler)
;              (gen-delegate (.ParameterType (aget (.GetParameters evt-method-info) 0))
;                            [s e] (binding [*cur* target] (handler s e)))
;              handler)]
;     (.Invoke evt-method-info target (to-array [dg]))
;     dg))

; (defn event-helper [target event-key handler prefix]
;   (let [mname (str prefix (name event-key))]
;     (if-let [m (.GetMethod (.GetType target) mname)]
;       (event-dg-helper target m handler)
;       (throw (System.MissingMethodException. (str (.GetType target)) mname)))))

; (defn += [target event-key handler] (event-helper target event-key handler "add_"))

; (defn -= [target event-key handler] (event-helper target event-key handler "remove_"))

; (defn command-binding
;   ([command exec-fn can-exec-fn]
;      (CommandBinding. command
;                       (gen-delegate ExecutedRoutedEventHandler [s e] (exec-fn s e))
;                       (when can-exec-fn
;                         (gen-delegate CanExecuteRoutedEventHandler [s e] (can-exec-fn s e)))))
;   ([command exec-fn]
;      (command-binding command exec-fn nil)))

; (defn get-static-field [type fname]
;   (when-let [f (.GetField type fname (enum-or BindingFlags/Static BindingFlags/Public))]
;     (.GetValue f nil)))

; (defn get-static-field-throw [type fname]
;   (or (get-static-field type fname) (throw (System.MissingFieldException. (str type) fname))))

; (defn find-dep-prop [type key]
;   (get-static-field type (str (name key) "Property")))

; (defn find-routed-event [type key]
;   (get-static-field type (str (name key) "Event")))

; (defn bind [target key binding]
;   (let [dep-prop (if (instance? DependencyProperty key) key (find-dep-prop (type target) key))]
;     (BindingOperations/SetBinding target dep-prop binding)))

; (declare caml-compile)

; (defn gen-invoke [method-str sym & args]
;   (let [method-sym (symbol (str "." method-str))]
;     `(~method-sym ~sym ~@args)))

; (defn when-type? [t] (comment (eval `(clojure.core/when (clojure.core/instance? System.Type ~t) ~t))))

; (defn get-xaml-type [^Type type]
;   (.GetXamlType (XamlReader/GetWpfSchemaContext) type))

; (defn get-type-converter [^Type type]
;   (when-let [xaml-type (get-xaml-type type)]
;     (when-let [type-converter (.TypeConverter xaml-type)]
;       (.ConverterType type-converter))))

; (defn convert-from [cls-type type-converter value]
;   (when value (if type-converter
;                 (if (instance? cls-type value)
;                   value
;                   (let [tc (Activator/CreateInstance type-converter)]
;                     (when (instance? TypeConverter tc)
;                       (when (.CanConvertFrom tc (type value))
;                         (.ConvertFrom tc value)))))
;                 (cast cls-type value))))

; (defn pset-property-handler [^Type type ^PropertyInfo prop-info target value]
;   (let [ptype (.PropertyType prop-info)
;         type-converter (get-type-converter ptype)]
;     (if (instance? BindingBase value)
;       (bind target (.Name prop-info) value)
;       (if (.CanWrite prop-info)
;         (let [res (if (fn? value)
;                     (value (.GetValue prop-info target nil))
;                     value)
;               res (convert-from ptype type-converter value)]
;           (.SetValue prop-info target res nil))
;         (if (fn? value)
;           (value (.GetValue prop-info target nil))
;           (let [^ICollection coll (.GetValue prop-info target nil)]
;             (.Clear coll)
;             (doseq [x value] (.Add coll x))))))))
; (defn pset-event-handler [^Type type ^EventInfo event-info target value]
;   (event-dg-helper target (.GetAddMethod event-info) value))

; (defn pset-method-handler [^Type type ^MethodInfo method-info target value]
;   (.Invoke method-info target (to-array value)))

; (defn pset-handle-member-key [^Type type name target val]
;   (let [members (.GetMember type name)]
;     (if-let [member (first members)]
;       (do
;         (cond
;          (instance? PropertyInfo member) (pset-property-handler type member target val)
;          (instance? EventInfo member) (pset-event-handler type member target val)
;          (instance? MethodInfo member) (pset-method-handler type member target val)
;          :default (throw (InvalidOperationException. (str "Don't know how to handle " member " on " type)))))
;       (throw (MissingMemberException. (str type) name)))))

; (defn pset-attached-prop-setter-handler [^Type type ^MethodInfo method-info target value]
;   (let [ptype (.ParameterType (second (.GetParameters method-info)))
;         type-converter (get-type-converter ptype)]
;     (.Invoke method-info nil (to-array [target (convert-from ptype type-converter value)]))))

; (defn pset-handle-attached-property [^Type type attached-type attached-prop target val]
;   (if *xaml-schema-ctxt*
;     (if-let [xaml-type (resolve-xaml-type *xaml-schema-ctxt* attached-type)]
;       (if-let [member (.GetAttachableMember xaml-type attached-prop)]
;         (pset-attached-prop-setter-handler
;          type (.. member Invoker UnderlyingSetter) target val)
;         (throw (Exception. (str "Unable to find attached property " attached-prop " on type " attached-type))))
;       (throw (Exception. (str "Unable to find xaml type " attached-type))))
;     (throw (Exception. "No *xaml-schema-ctxt*"))))

; (defn pset-handle-keyword [^Type type key target val]
;   (let [key (name key)
;         dot-parts (str/split key #"\.")]
;     (cond
;      (> (count dot-parts) 1) (pset-handle-attached-property type (first dot-parts) (second dot-parts) target val)
;                                         ;(= "*cur*" key) `(~val ~target)
;      :default (pset-handle-member-key type key target val))))

; (defn pset-handle-key [^Type type key target val]
;   (cond
;    (keyword? key) (pset-handle-keyword type key target val)
;                                         ;(instance? AttachedData key) `(wpf/attach ~key ~target ~val)
;                                         ;(instance? DependencyProperty key) (throw (NotImplementedException.))
;    :default (throw (ArgumentException. (str "Don't know how to handle key " key)))))

; (defn caml-form? [x] (and (list? x) (keyword? (first x))))

; (defn pset-compile-val [val]
;   (cond
;    (caml-form? val) (caml-compile val)
;    (vector? val) (vec (for [x val]
;                         (if (caml-form? x) (caml-compile x) x)))
;    :default val))

; (defn pset-exec-setter [type-sym target-sym key value]
;   `(let [val# ~(wpf/pset-compile-val value)]
;      (wpf/pset-handle-key ~type-sym ~key ~target-sym val#)))

; (defn pset-exec-setters [type-sym target-sym setters]
;   (for [[key val] (partition 2 setters)]
;     (pset-exec-setter type-sym target-sym key val)))

; (defn pset* [target setters]
;   (let [target-type (.GetType target)]
;     (binding [*cur* target]
;       (doseq [[k v] (partition 2 setters)]
;         (pset-handle-key target-type k target v))
;       target)))

; (defn pset-compile [target setters]
;   (let [target-sym (gensym "t")
;         type-sym (gensym "type")]
;     `(let [~target-sym ~target
;            ~type-sym (.GetType ~target-sym)]
;        (binding [wpf/*cur* ~target-sym]
;          ~@(pset-exec-setters type-sym target-sym setters)
;          ~target-sym))))

; (defmacro pset! [& forms]
;   (let [type-target? (first forms)
;         type (when-type? type-target?)
;         target (if type (second forms) type-target?)
;         setters (if type (nnext forms) (next forms))]
;     (pset-compile target setters)))

; (defmacro defattached [name & opts]
;   (let [qname (str *ns* "/" (clojure.core/name name))]
;     `(clojure.core/defonce ~name
;        (wpf/create-attached-data
;         (System.Windows.DependencyProperty/RegisterAttached
;          ~qname System.Object System.Object
;          (wpf/pset! (System.Windows.FrameworkPropertyMetadata.)
;                                 :Inherits true ~@opts))))))

; (defattached cur-view)

; (defn split-attrs-forms [forms]
;   (let [was-key (atom false)]
;     (split-with (fn [x]
;                   (cond
;                    @was-key (do (reset! was-key false) true)
;                    (list? x) false
;                    :default (do (reset! was-key true) true))) forms)))

; (defn at-compile [target forms]
;   (let [[target-attrs forms] (split-attrs-forms forms)
;         tsym (gensym "t")
;         xforms (for [form forms]
;                  (let [path (first form)
;                        setters (rest form)]
;                    (at-compile `(wpf/find-elem-warn ~tsym ~path) setters)))
;         pset-expr (pset-compile tsym target-attrs)]
;     `(do (let [~tsym ~target] ~pset-expr
;               ~@xforms))))

; (defmacro at [target & forms]
;   (let [[dispatcher-obj target] (compile-target-expr target)
;         at-expr (at-compile target forms)]
;     `(wpf/with-invoke ~dispatcher-obj
;        ~at-expr)))

; (defmacro async-at [target & forms]
;   (let [[dispatcher-obj target] (compile-target-expr target)
;         at-expr (at-compile target forms)]
;     `(wpf/with-begin-invoke ~dispatcher-obj
;        ~at-expr)))

; (defn xaml-ns
;   [ns-name asm-name]
;   (if-let [asm (assembly-load asm-name)]
;     {:ns (str "clr-namespace:" ns-name ";assembly=" asm-name)
;      :context (XamlSchemaContext. [asm])}
;     (throw (Exception. ("Unable to load assembly " asm-name)))))

; (defn caml-children-expr [ns-ctxt ^XamlType xt ^Type type elem-sym children]
;   (when (and (sequential? children) (seq children))
;     (let [children* (vec (for [ch children]
;                            (if (caml-form? ch) (caml-compile ns-ctxt ch) ch)))
;           cp (.get_ContentProperty xt)
;           member (.get_UnderlyingMember cp)
;           cp-xt (.Type cp)
;           is-collection (.IsCollection cp-xt)]
;       (if (instance? PropertyInfo member)
;         (let [val-sym (gensym "val")
;               expr `(pset-handle-key ~type ~(keyword (.Name member)) ~elem-sym ~val-sym)]
;           (if is-collection
;             `(let [~val-sym (clojure.core/first ~children*)] ~expr)
;             `(let [~val-sym ~children*] ~expr)))
;         (throw (ex-info (str "Unable to find conent property for" xt) {}))))))

; (defn caml-compile
;   ([form] (caml-compile nil form))
;   ([ns-ctxt form]
;      (let [ns-ctxt (or ns-ctxt default-xaml-context)]
;        (binding [*xaml-schema-ctxt* ns-ctxt]
;          (let [nexpr (name (first form))
;                enparts (str/split nexpr #"#")
;                nexpr (first enparts)
;                ename (when (> (count enparts) 1) (second enparts))
;                xt (resolve-xaml-type ns-ctxt nexpr)]
;            (if xt
;              (let [type (.get_UnderlyingType xt)
;                    elem-sym (with-meta (gensym "e") {:tag type})
;                    ctr-sym (symbol (str (.FullName type) "."))
;                    forms (if ename [`(.set_Name ~elem-sym ~ename)] [])
;                    more (rest form)
;                    attrs? (first more)
;                    pset-expr (when (vector? attrs?)
;                                (pset-compile elem-sym attrs?))
;                    forms (if pset-expr (conj forms pset-expr) forms)
;                    children (if pset-expr (rest more) more)
;                    children-expr (caml-children-expr ns-ctxt xt type elem-sym children)
;                    forms (if children-expr (conj forms children-expr) forms)
;                    ]
;                `(let [~elem-sym (~ctr-sym)]
;                   ~@forms
;                   ~elem-sym))
;              (throw (ex-info (str "Unable to resolve Xaml type " nexpr) {}))))))))

; (declare caml*)

; (defn caml-children* [ns-ctxt ^XamlType xt ^Type type parent children]
;   (when (and (sequential? children) (seq children))
;     (let [children* (vec (for [ch children]
;                            (if (caml-form? ch) (caml* ns-ctxt ch) ch)))
;           cp (.get_ContentProperty xt)
;           member (.get_UnderlyingMember cp)
;           cp-xt (.Type cp)
;           is-collection (.IsCollection cp-xt)]
;       (if (instance? PropertyInfo member)
;         (pset-property-handler type member parent
;                                (if is-collection children* (first children*)))
;         (throw (ex-info (str "Unable to find conent property for" xt) {}))))))

; (defn caml*
;   ([ns-ctxt & form]
;      (let [ns-ctxt (or ns-ctxt default-xaml-context)]
;        (binding [*xaml-schema-ctxt* ns-ctxt]
;          (let [nexpr (name (first form))
;                enparts (str/split nexpr #"#")
;                nexpr (first enparts)
;                ename (when (> (count enparts) 1) (second enparts))
;                xt (resolve-xaml-type ns-ctxt nexpr)]
;            (if xt
;              (let [type (.get_UnderlyingType xt)
;                    inst (Activator/CreateInstance type)
;                    more (rest form)
;                    attrs? (first more)
;                    attrs (when (vector? attrs?) attrs?)
;                    children (if attrs (rest more) more)]
;                (when ename (set! (.Name inst) ename))
;                (pset* inst attrs)
;                (caml-children* ns-ctxt xt type inst children)
;                inst)
;              (throw (ex-info (str "Unable to resolve Xaml type " nexpr) {}))))))))


; (defn caml-children* [parent xaml-type children])

; (defn caml-compile2 [target & more]
;   (let [attrs? (first more)
;         attrs (vec (when (vector? attrs?) attrs?))
;         children (if attrs (rest more) more)
;         children (for [ch children]
;                    (if (caml-form? ch)
;                      (caml-compile2 ch)
;                      ch))]
;     `(pset! (caml* ~target) ~@attrs)))

; (defmacro caml [& form]
;   (let [x (first form)
;         ns-map (when (map? x) (eval x))
;         ns-ctxt (merge *xaml-schema-ctxt* ns-map)
;         form (if ns-map (rest form) form)]
;     (caml-compile ns-ctxt form)))

; (defattached dev-sandbox-refresh)

; (defn set-sandbox-refresh [sandbox func]
;   (let [window (:window sandbox)]
;     (doat window
;           (attach dev-sandbox-refresh window (fn [] (at window :Content (func))))
;           (.Execute System.Windows.Input.NavigationCommands/Refresh nil window))))

; (defn sandbox-refresh [s e]
;   (binding [*cur* s]
;     (when-let [on-refresh @dev-sandbox-refresh]
;       (binding [*dev-mode* true] (on-refresh)))))

; (defn dev-sandbox [& options]
;   (let [sandbox (apply separate-threaded-window options)
;         window (:window sandbox)
;         opts (apply hash-map options)
;         {:keys [refresh title]} opts]
;     (at window
;         :CommandBindings (fn [bindings]
;                            (.Add bindings
;                                  (command-binding
;                                   System.Windows.Input.NavigationCommands/Refresh
;                                   #'sandbox-refresh))))
;     (when title (at window :Title title))
;     (when refresh (set-sandbox-refresh sandbox refresh))
;     sandbox))

; (defn dev-init [refresh & options]
;   (def sand (apply dev-sandbox options))
;   (def wind (:window sand))
;   (at wind :Height 768.0 :Width 1024.0)
;   (set-sandbox-refresh sand refresh))

; (defn get-app-main-window []
;   (when-let [app Application/Current]
;     (doat app (.MainWindow *cur*))))

; (defn resource-uri [assembly-name path]
;   (str "pack://application:,,,/" assembly-name ";component" path))

(defn merge-window-resource [dict]
  (..  (wpf/get-app-main-window) Resources MergedDictionaries (Add dict)))

(defn find-elem [elem-name-key]
   (.FindName (wpf/get-app-main-window) (name elem-name-key)))

(prn "wpf loaded")
