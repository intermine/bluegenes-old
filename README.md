# bluegenes

A [re-frame](https://github.com/Day8/re-frame) application designed to function as a UI for [InterMine's](http://www.intermine.org) data warehouse layer.

## Development Mode

### Run application:

```
lein foreign
lein clean
bower install
lein figwheel dev
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

If you wish to use a repl from your terminal, consider installing [rlwrap](https://github.com/hanslub42/rlwrap) for a more user-friendly experience with arrow keys that work rather than generating ]]^A gobbledeygook, and run `rlwrap lein figwheel dev` rather than just `lein figwheel dev`.

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
