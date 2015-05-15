weighted_calibration_point = function(predicted_position = x, covariance_pred_position = P) {
  locs = as.matrix(expand.grid(seq(0, 10, by = 1), seq(0, 8, by = 1)))
  beta = numeric(nrow(locs))
  for (i in 1:nrow(locs)) {
     beta[i] = exp(-1/2 *(t(matrix(locs[i,]) - predicted_position) %*% solve(covariance_pred_position) %*% (matrix(locs[i,]) - predicted_position)))/sqrt(det(covariance_pred_position) * 2 * pi)
  }
  return(data.frame(x = locs[,1], y = locs[,2], beta))
}

# Measurement is a dataframe of addresses and RSSI measurements at time t
kalman = function(x, P, measurement, Q, radio_map) {
  # Remove all values from the radio map with 0 variance
  radio_map = radio_map %>% filter(variance > 0)
  
  # F is the identity
  F = diag(1, nrow = 2)

  # PREDICT x, P
  x = F %*%  x
  P = F %*% P %*% t(F) + Q

  # Calculate various uncertainties
  beta = weighted_calibration_point(predicted_position = x, covariance_pred_position = P)
  
  # Ensure we have the radio maps for the associated measurements
  measurement = measurement %>%
    filter(address %in% unique(radio_map$address))
  addresses = measurement$address
  
  y_hat = beta %>% 
    inner_join(radio_map, by = c("x", "y")) %>% 
    filter(address %in% addresses) %>%
    mutate(yk = beta * RSSI) %>% 
    group_by(address) %>% summarise(y_hat = sum(yk))
  
  p_hat = beta %>%
    mutate(x_hat = x * beta, y_hat = y * beta) %>%
    dplyr::select(x_hat, y_hat) %>%
    summarise(x_hat = sum(x_hat), y_hat = sum(y_hat)) %>%
    as.matrix() %>% t()
  
  Pxx = diag(0, nrow = 2)
  Pxy = matrix(rep(0, nrow(measurement) * 2), ncol = nrow(measurement), nrow = 2)
  Pyy = diag(0, nrow = nrow(measurement))
  for (i in 1:nrow(beta)) {
    p = t(data.matrix(beta[i,c(1,2)]))
    a = radio_map %>%
      filter(x == p[1,] & y == p[2,]) %>%
      inner_join(y_hat, by = "address") %>%
      mutate(a = RSSI - y_hat)
    Pa = diag(a$variance, nrow = nrow(a))
    
    Pxx = Pxx + beta[i,]$beta/sum(beta$beta) * (diag(0.5**2/12, nrow = 2) + (p - p_hat) %*% t(p - p_hat))
    Pxy = Pxy + beta[i,]$beta/sum(beta$beta) * (p - p_hat) %*% t(a$a)
    Pyy = Pyy + beta[i,]$beta/sum(beta$beta) * (Pa + (a$a) %*% t(a$a))
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
kalman_runner = function(data, radioMaps, x, P) {
  times = as.character(data$timeWindow %>% unique())
  filtered = matrix(rep(0, length(times)*2), ncol = 2)
  
  # There is most likely a more efficient way of performing this
  for (j in 1:length(times)) {
    measurement = data[data$timeWindow == times[j], c("address","medianRssi")]
    output = kalman(x = x, P = P, measurement = measurement, Q = diag(0.1, nrow = 2), radio_map = radioMaps)
    
    x = output[[1]]
    P = output[[2]]
    filtered[j,] = x
  }
  return(data.frame(filtered_x = filtered[,1], filtered_y = filtered[,2]))
}
