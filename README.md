# bluegenes

A [re-frame](https://github.com/Day8/re-frame) application designed to ... well, that part is up to you.

## Development Mode

### Run application:

```
lein foreign
lein clean
lein bower install
lein figwheel dev
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

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
