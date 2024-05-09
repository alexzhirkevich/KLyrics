./gradlew jsBrowserDistribution || return -1
rm -rf ./composeResources
copy ./example/webApp/build/dist/js/productionExecutable/* .
rm -rf ./META-INF
git add .
git commit -m "deploy"
git push origin gh-pages