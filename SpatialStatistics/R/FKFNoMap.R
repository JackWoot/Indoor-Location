weighted_calibration_point_nomap = function(predicted_position = x, covariance_pred_position = P, training = train) {
  locs = training %>%
    dplyr::select(x,y) %>%
    unique() %>%
    data.matrix()
  
  beta = numeric(nrow(locs))
  for (i in 1:nrow(locs)) {
     beta[i] = exp(-1/2 *(t(matrix(locs[i,]) - predicted_position) %*% solve(covariance_pred_position) %*% (matrix(locs[i,]) - predicted_position)))/sqrt(det(covariance_pred_position) * 2 * pi)
  }
  return(data.frame(x = locs[,1], y = locs[,2], beta))
}

# Kalman Filter without radio map
# We need to interpolate somehow, I can't see how else this will work
# We can add to only the parts of the matrices that are relevant to that point, I think this makes sense
kalman_nomap = function(x, P, measurement, Q, training) {
  estimotesByLocation = training %>% 
    group_by(x,y,address) %>% 
    summarise(averageRssi = round(mean(rssi), 0)) %>% 
    spread(address, averageRssi) %>%
    gather(address, averageRssi, -x, -y)
  
  train = training %>%
    group_by(x,y,address) %>%
    summarise(sdRssi = round(sd(rssi), 2))
  
  train = estimotesByLocation %>% 
    left_join(train, by = c("x", "y", "address")) 
  
  # F is the identity
  F = diag(1, nrow = 2)

  # PREDICT x, P
  x = F %*%  x
  P = F %*% P %*% t(F) + Q

  # Calculate various uncertainties
  beta = weighted_calibration_point_nomap(predicted_position = x, covariance_pred_position = P, training = training)
  
  # Ensure we have the training data for the associated measurements
  measurement = measurement %>%
    filter(address %in% unique(train$address))
  addresses = measurement$address
  
  y_hat = beta %>% 
    inner_join(train, by = c("x", "y")) %>% 
    filter(address %in% addresses) %>%
    mutate(yk = beta * averageRssi) %>% 
    group_by(address) %>% summarise(y_hat = sum(yk, na.rm = T))
  
  p_hat = beta %>%
    mutate(x_hat = x * beta, y_hat = y * beta) %>%
    dplyr::select(x_hat, y_hat) %>%
    summarise(x_hat = sum(x_hat), y_hat = sum(y_hat)) %>%
    as.matrix() %>% t()
  
  Pxx = diag(0, nrow = 2)
  Pxy = matrix(rep(0, nrow(measurement) * 2), ncol = nrow(measurement), nrow = 2)
  Pyy = diag(0, nrow = nrow(measurement))
  # Build the matrices for every position
  positions = beta %>% group_by(x,y) %>% summarise() %>% data.matrix()
  for(i in 1:nrow(positions)) {
    p = positions[i,]
    a = train %>%
      filter(x == p[1], y == p[2]) %>%
      inner_join(y_hat, by = "address") %>%
      mutate(a = averageRssi - y_hat)
    # Literally adding some zeroes here... not sure if this is the correct approach
    a[is.na(a)] = 0
    b = beta[beta$x == p[1] & beta$y == p[2],]$beta/sum(beta$beta)
    
    Pa = diag(a$sdRssi**2, nrow = nrow(a))
    Pxx = Pxx + b * (diag(0.5**2/12, nrow = 2) + (p - p_hat) %*% t(p - p_hat))
    Pxy = Pxy + b * ((p - p_hat) %*% t(a$a))
    Pyy = Pyy + b * (Pa + (a$a) %*% t(a$a))
  }
   
  # The difference between the estimate and the observation by address
  diff = y_hat %>% 
    inner_join(measurement, by = "address") %>%
    mutate(diff = y_hat - medianRssi)
  
  # UPDATE
    x = x + Pxy %*% solve(Pyy) %*% (diff$diff)
    P = Pxx - Pxy %*% solve(Pyy) %*% t(Pxy)
    
    return(list(x,P))
}

# Pass median Filtered data frame to this function, along with radio maps
kalman_runner_nomap = function(data, training, x, P) {
  times = as.character(data$timeWindow %>% unique())
  filtered = matrix(rep(0, length(times)*2), ncol = 2)
  
  # There is most likely a more efficient way of performing this
  for (j in 1:length(times)) {
    measurement = data[data$timeWindow == times[j], c("address","medianRssi")]
    output = kalman_nomap(x = x, P = P, measurement = measurement, Q = diag(1, nrow = 2), training = training)
    
    x = output[[1]]
    P = output[[2]]
    print(list(x,P))
    filtered[j,] = x
  }
  return(data.frame(filtered_x = filtered[,1], filtered_y = filtered[,2]))
}