Retired in favour of BlueGenes: https://github.com/intermine/bluegenes 


# bluegenes

A [re-frame](https://github.com/Day8/re-frame) application designed to function as a UI for [InterMine's](http://www.intermine.org) data warehouse layer.

## Development Mode

### Environment Variables

A few environment variables are needed at runtime. Do not check these into your source code.

| Environment Variable 	| Purpose 	|
|------------------------	|---------------------------------------------------------------------------------------------------------------------------------------------------------	|
| ```MONGO_URL``` 	| A MongoDB URI for storing data. Ex: ```mongodb://localhost:27017/intermine``` 	|
| ```GOOGLE-CLIENT-ID``` 	| A Google Client ID that is used to authenticate users. A Google Project is required which can be configured at (https://console.developers.google.com/). [Creating a Google Developers Console project and client ID](https://developers.google.com/identity/sign-in/web/devconsole-project) walks you through the steps needed to get an ID. 	|

When running locally, consider creating a ```~/.lein/profiles.clj``` file with the following values:

```
{:user
 {:env
  {:mongo-url "mongodb://localhost:27017/monger-test"
   :google-client-id "37046834670348634608364.googleid"}}}
```

These values will be automatically applied as environment variables to the project at runtime.


### Run application:

```
lein foreign
lein clean
bower install
lein figwheel dev
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

If you wish to use a repl from your terminal, consider installing [rlwrap](https://github.com/hanslub42/rlwrap) for a more user-friendly experience with arrow keys that work rather than generating ASCII codes, and run `rlwrap lein figwheel dev` rather than just `lein figwheel dev`.

### Compile Less to CSS:
Currently this isn't built into the lein / figwheel lifecycle and needs to be done manually (this may change!).

Assuming you have [Less](http://lesscss.org/#download-options) and [Less clean CSS plugin](https://github.com/less/less-plugin-clean-css#lessc-usage) installed globally, from the root directory run:

   ```
   lessc --clean-css src/less/style.less resources/public/css/style.css
   ```


### Run tests:

```
lein clean
lein doo phantom test once
```

The above command assumes that you have [phantomjs](https://www.npmjs.com/package/phantomjs) installed. However, please note that [doo](https://github.com/bensu/doo) can be configured to run cljs.test in many other JS environments (chrome, ie, safari, opera, slimer, node, rhino, or nashorn).

## Production Build

```
lein foreign
lein clean
lein cljsbuild once min
```

## Docker

To build an image locally:
```
docker build -t bluegenes:[version tag]
```

To run a container from the image
```
docker run -it --rm -p 8080:5000 --name bluegenes-server bluegenes:[version tag]
```

## Deploy to Dokku

If not done already, add your dokku server as a remote:
```
git remote add dokkuserver dokku@example.com:bluegenes
```

Push the repository:
```
git push dokkuserver master
```
